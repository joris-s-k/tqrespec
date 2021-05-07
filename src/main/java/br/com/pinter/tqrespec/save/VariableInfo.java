/*
 * Copyright (C) 2021 Emerson Pinter - All Rights Reserved
 */

/*    This file is part of TQ Respec.

    TQ Respec is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    TQ Respec is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with TQ Respec.  If not, see <http://www.gnu.org/licenses/>.
*/

package br.com.pinter.tqrespec.save;

import com.google.common.io.BaseEncoding;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

@SuppressWarnings("unused")
public class VariableInfo implements Serializable {
    private String name = null;
    private String alias = null;
    private int keyOffset = -1;
    private int valOffset = -1;
    private int valSize = -1;
    private String valueString = null;
    private Integer valueInteger = null;
    private Float valueFloat = null;
    private byte[] valueByteArray = null;
    private VariableType variableType;
    private int blockOffset = -1;
    private int utf16SizeBytes = 2;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAlias() {
        if (StringUtils.isNotBlank(alias)) {
            return alias;
        }
        return name;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public int getKeyOffset() {
        return keyOffset;
    }

    public void setKeyOffset(int keyOffset) {
        this.keyOffset = keyOffset;
    }

    public int getValOffset() {
        return valOffset;
    }

    public void setValOffset(int valOffset) {
        this.valOffset = valOffset;
    }

    /**
     * Returns the value size. Strings in UTF8 and UTF16 encodings will return the same size.
     */
    public int getValSize() {
        return valSize;
    }

    public void setValSize(int valSize) {
        this.valSize = valSize;
    }

    public Object getValue() {
        if (variableType == VariableType.INTEGER)
            return valueInteger;
        if (variableType == VariableType.STRING || variableType == VariableType.STRING_UTF_16_LE)
            return valueString;
        if (variableType == VariableType.FLOAT)
            return valueFloat;
        if (variableType == VariableType.UID || variableType == VariableType.STREAM)
            return valueByteArray;
        return null;
    }

    /**
     * Returns value as a string. Bytes are converted to hex-string.
     */
    public String getValueString() {
        if (variableType == VariableType.INTEGER)
            return String.valueOf(valueInteger);
        if (variableType == VariableType.STRING || variableType == VariableType.STRING_UTF_16_LE)
            return valueString;
        if (variableType == VariableType.FLOAT)
            return String.valueOf(valueFloat);
        if (variableType == VariableType.UID)
            return UID.convertUidByteToString(valueByteArray);
        if (variableType == VariableType.STREAM)
            return BaseEncoding.base16().encode(valueByteArray);
        return null;
    }

    /**
     * Returns value length in bytes
     */
    public int getValBytesLength() {
        int sz = valSize;
        if (variableType == VariableType.STRING_UTF_16_LE) {
            sz *= utf16SizeBytes;
        }
        return sz;
    }

    /**
     * Returns total variable length in bytes. Are considered first bytes specifying variable name length,
     * variable name, first bytes specifying value length if present, value (double if utf16)
     */
    public int getVariableBytesLength() {
        if (valOffset == -1) {
            return name.length() + 4;
        }

        return valOffset - keyOffset + getValBytesLength() + getValuePrefix();
    }

    private int getValuePrefix() {
        int valSizePrefix = 0;

        if (variableType == VariableType.STRING || variableType == VariableType.STRING_UTF_16_LE || variableType == VariableType.STREAM) {
            valSizePrefix = 4;
        }

        return valSizePrefix;
    }

    public void setValue(String value) {
        this.valueString = value;
    }

    public void setValue(int value) {
        this.valueInteger = value;
    }

    public void setValue(float value) {
        this.valueFloat = value;
    }

    public void setValue(byte[] value) {
        this.valueByteArray = value;
    }

    public VariableType getVariableType() {
        return variableType;
    }

    public void setVariableType(VariableType variableType) {
        this.variableType = variableType;
    }

    public int getBlockOffset() {
        return blockOffset;
    }

    public void setBlockOffset(int blockOffset) {
        this.blockOffset = blockOffset;
    }

    public static Builder builder() {
        return new Builder();
    }
    public static class Builder {
        private String builderName = null;
        private String builderAlias = null;
        private int builderKeyOffset = -1;
        private int builderValOffset = -1;
        private int builderValSize = -1;
        private String builderValueString = null;
        private Integer builderValueInteger = null;
        private Float builderValueFloat = null;
        private byte[] builderValueByteArray = null;
        private VariableType builderVariableType;
        private int builderBlockOffset = -1;
        private int builderUtf16SizeBytes = 2;

        public Builder name(String builderName) {
            this.builderName = builderName;
            return this;
        }

        public Builder alias(String builderAlias) {
            this.builderAlias = builderAlias;
            return this;
        }

        public Builder keyOffset(int builderKeyOffset) {
            this.builderKeyOffset = builderKeyOffset;
            return this;
        }

        public Builder valOffset(int builderValOffset) {
            this.builderValOffset = builderValOffset;
            return this;
        }

        public Builder valSize(int builderValSize) {
            this.builderValSize = builderValSize;
            return this;
        }

        public Builder value(String value) {
            this.builderValueString = value;
            return this;
        }

        public Builder value(int value) {
            this.builderValueInteger = value;
            return this;
        }

        public Builder value(float value) {
            this.builderValueFloat = value;
            return this;
        }

        public Builder value(byte[] value) {
            this.builderValueByteArray = value;
            return this;
        }

        public Builder variableType(VariableType builderVariableType) {
            this.builderVariableType = builderVariableType;
            return this;
        }

        public Builder blockOffset(int builderBlockOffset) {
            this.builderBlockOffset = builderBlockOffset;
            return this;
        }

        public Builder utf16SizeBytes(int utf16SizeBytes) {
            this.builderUtf16SizeBytes = utf16SizeBytes;
            return this;
        }

        public VariableInfo build() {
            if(builderUtf16SizeBytes !=2 && builderUtf16SizeBytes != 4) {
                builderUtf16SizeBytes = 2;
            }

            VariableInfo v = new VariableInfo();
            v.name = builderName;
            v.blockOffset = builderBlockOffset;
            v.alias = builderAlias;
            v.keyOffset = builderKeyOffset;
            v.valOffset = builderValOffset;
            v.valSize = builderValSize;
            v.valueString = builderValueString;
            v.valueInteger = builderValueInteger;
            v.valueFloat = builderValueFloat;
            v.valueByteArray = builderValueByteArray;
            v.variableType = builderVariableType;
            v.utf16SizeBytes = builderUtf16SizeBytes;

            if(v.valSize == -1) {
                if(v.variableType.equals(VariableType.FLOAT) || v.variableType.equals(VariableType.INTEGER)) {
                    v.valSize = 4;
                } else if(v.variableType.equals(VariableType.UID)) {
                    v.valSize = 16;
                } else if(v.variableType.equals(VariableType.STREAM) && v.valueByteArray != null) {
                    v.valSize = v.valueByteArray.length;
                } else if(v.variableType == VariableType.STRING && v.valueString != null) {
                    v.valSize = v.valueString.length();
                } else if(v.variableType == VariableType.STRING_UTF_16_LE && v.valueString != null) {
                    v.valSize = v.valueString.length() * builderUtf16SizeBytes;
                }
            }

            if(v.valOffset == -1) {
                v.valOffset = builderKeyOffset + 4 + v.name.length() + v.getValuePrefix();
            }
            return v;
        }
    }

    @Override
    public String toString() {
        return String.format("name={%s}; alias={%s}; value={%s}; keyOffset={%d}, valOffset={%d}; valSize={%d}; variableType: {%s}", this.name, alias, this.getValue(), this.keyOffset, this.valOffset, this.valSize, variableType);
    }
}
