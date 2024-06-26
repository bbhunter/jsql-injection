import com.jsql.util.TamperingUtil
import spock.lang.Specification

class TamperingUtilSpock extends Specification {
    
    def tamperingUtil
    String result

    def 'Check Base64'() {
        
        when: tamperingUtil.withBase64()
        and: result = tamperingUtil.tamper(TamperingUtil.TAG_OPENED + '123abc' + TamperingUtil.TAG_CLOSED)
        then: result == 'MTIzYWJj'
    }

    def 'Check VersionComment'() {
        
        when: tamperingUtil.withVersionComment()
        and: result = tamperingUtil.tamper(TamperingUtil.TAG_OPENED + 'aconcat(b c,d,`e`)f,%2b,0x06' + TamperingUtil.TAG_CLOSED)
        then: result == '/*!aconcat*/(b c,d,`e`)f,%2b,0x06'
    }

    def 'Check FunctionComment'() {
        
        when: tamperingUtil.withFunctionComment()
        and: result = tamperingUtil.tamper(TamperingUtil.TAG_OPENED + 'aconcat(b)' + TamperingUtil.TAG_CLOSED)
        then: result == 'aconcat/**/(b)'
    }

    def 'Check EqualToLike'() {
        
        when: tamperingUtil.withEqualToLike()
        and: result = tamperingUtil.tamper(TamperingUtil.TAG_OPENED + 'a=b' + TamperingUtil.TAG_CLOSED)
        then: result == 'a+like+b'
    }

    def 'Check RandomCase'() {
        
        when: tamperingUtil.withRandomCase()
        and: result = tamperingUtil.tamper(TamperingUtil.TAG_OPENED + 'abcdefghijklmnopqrstuvwxyz' + TamperingUtil.TAG_CLOSED)
        then:
            ![
                'abcdefghijklmnopqrstuvwxyz',
                'ABCDEFGHIJKLMNOPQRSTUVWXYZ',
            ].contains(result)
            result.toLowerCase() == 'abcdefghijklmnopqrstuvwxyz'
            result.toUpperCase() == 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'
    }

    def 'Check StringToChar ab'() {
        
        when: tamperingUtil.withStringToChar()
        and: result = tamperingUtil.tamper(TamperingUtil.TAG_OPENED + "'ab'" + TamperingUtil.TAG_CLOSED)
        then: result == 'concat(char(97),char(98))'
    }

    def 'Check HexToChar AB'() {
        
        when: tamperingUtil.withHexToChar()
        and: result = tamperingUtil.tamper(TamperingUtil.TAG_OPENED + '0x4142' + TamperingUtil.TAG_CLOSED)
        then: result == 'concat(char(65),char(66))'
    }

    def 'Check QuoteToUtf8'() {
        
        when: tamperingUtil.withQuoteToUtf8()
        and: result = tamperingUtil.tamper(TamperingUtil.TAG_OPENED + "'" + TamperingUtil.TAG_CLOSED)
        then: result == '%ef%bc%87'
    }
    
    
    // Multiple
    
    def 'Check VersionComment+FunctionComment'() {
        
        when: tamperingUtil.withVersionComment().withFunctionComment()
        and: result = tamperingUtil.tamper(TamperingUtil.TAG_OPENED + 'aconcat(b)' + TamperingUtil.TAG_CLOSED)
        then: result == 'aconcat/**/(b)'
    }
    
    def 'Check VersionComment+FunctionComment+HexToChar'() {
        
        when: tamperingUtil.withVersionComment().withFunctionComment().withHexToChar()
        and: result = tamperingUtil.tamper(TamperingUtil.TAG_OPENED + 'aconcat(0x4142)' + TamperingUtil.TAG_CLOSED)
        then: result == 'aconcat/**/(concat/**/(char/**/(65),char/**/(66)))'
    }
    
    
    // Comments
    
    def 'Check SpaceToMultilineComment'() {
        
        when: tamperingUtil.withSpaceToMultilineComment()
        and: result = tamperingUtil.tamper(TamperingUtil.TAG_OPENED + 'a+b' + TamperingUtil.TAG_CLOSED)
        then: result == 'a/**/b'
    }
    
    def 'Check SpaceToDashComment'() {
        
        when: tamperingUtil.withSpaceToDashComment()
        and: result = tamperingUtil.tamper(TamperingUtil.TAG_OPENED + 'a+b' + TamperingUtil.TAG_CLOSED)
        then: result == 'a--%0Ab'
    }
    
    def 'Check SpaceToSharpComment'() {
        
        when: tamperingUtil.withSpaceToSharpComment()
        and: result = tamperingUtil.tamper(TamperingUtil.TAG_OPENED + 'a+b' + TamperingUtil.TAG_CLOSED)
        then: result == 'a%23%0Ab'
    }
    
    
    // Special
    
    def 'Check no tampering'() {
        
        when: result = tamperingUtil.tamper(TamperingUtil.TAG_OPENED + 'abc' + TamperingUtil.TAG_CLOSED)
        then: result == 'abc'
    }
    
    def 'Check eval'() {
        
        when: tamperingUtil.withEval()
        and: tamperingUtil.customTamper = '''
            var tampering = function(sql) {
                return sql.replace(/a/, 'b')
            }            
        '''
        and: result = tamperingUtil.tamper(TamperingUtil.TAG_OPENED + 'a' + TamperingUtil.TAG_CLOSED)
        then: result == 'b'
    }
    
    def setup() {
        
        tamperingUtil = new TamperingUtil()
        result = null
    }
}