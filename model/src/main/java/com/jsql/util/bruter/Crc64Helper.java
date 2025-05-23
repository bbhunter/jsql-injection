/*******************************************************************************
 * Copyright (c) 2009, 2012 Mountainminds GmbH & Co. KG and Contributors
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Marc R. Hoffmann - initial API and implementation
 *
 *******************************************************************************/
package com.jsql.util.bruter;

/**
 * CRC64 checksum calculator based on the polynom specified in ISO 3309. The
 * implementation is based on the following publications:
 * 
 * <ul>
 * <li>http://en.wikipedia.org/wiki/Cyclic_redundancy_check</li>
 * <li>http://www.geocities.com/SiliconValley/Pines/8659/crc.htm</li>
 * </ul>
 */
public class Crc64Helper {
    
    private static final long POLY64REV = 0xd800000000000000L;
    
    private static final long[] LOOKUPTABLE;
    
    private Crc64Helper() {
        // Util class
    }

    static {
        LOOKUPTABLE = new long[0x100];
        for (int i = 0; i < 0x100; i++) {
            long v = i;
            for (int j = 0; j < 8; j++) {
                if ((v & 1) == 1) {
                    v = (v >>> 1) ^ Crc64Helper.POLY64REV;
                } else {
                    v = v >>> 1;
                }
            }
            Crc64Helper.LOOKUPTABLE[i] = v;
        }
    }

    /**
     * Calculates the CRC64 checksum for the given data array.
     * 
     * @param data
     *            data to calculate checksum for
     * @return checksum value
     */
    public static String generateCRC64(final byte[] data) {
        long sum = 0;
        for (byte element : data) {
            final int lookupidx = ((int) sum ^ element) & 0xff;
            sum = (sum >>> 8) ^ Crc64Helper.LOOKUPTABLE[lookupidx];
        }
        return String.valueOf(sum);
    }
}
