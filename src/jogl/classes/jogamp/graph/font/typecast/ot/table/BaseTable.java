/*
 * Typecast - The Font Development Environment
 *
 * Copyright (c) 2004-2007 David Schweinsberg
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jogamp.graph.font.typecast.ot.table;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;

/**
 * Baseline Table
 * @author <a href="mailto:david.schweinsberg@gmail.com">David Schweinsberg</a>
 */
public class BaseTable implements Table {
    
    private abstract static class BaseCoord {

        public abstract int getBaseCoordFormat();
        
        public abstract short getCoordinate();
    }
    
    private static class BaseCoordFormat1 extends BaseCoord {

        private short _coordinate;
        
        BaseCoordFormat1(DataInput di) throws IOException {
            _coordinate = di.readShort();
        }

        public int getBaseCoordFormat() {
            return 1;
        }
        
        public short getCoordinate() {
            return _coordinate;
        }
        
    }
    
    private static class BaseCoordFormat2 extends BaseCoord {

        private short _coordinate;
        private int _referenceGlyph;
        private int _baseCoordPoint;
        
        BaseCoordFormat2(DataInput di) throws IOException {
            _coordinate = di.readShort();
            _referenceGlyph = di.readUnsignedShort();
            _baseCoordPoint = di.readUnsignedShort();
        }

        public int getBaseCoordFormat() {
            return 2;
        }
        
        public short getCoordinate() {
            return _coordinate;
        }
        
    }
    
    private static class BaseCoordFormat3 extends BaseCoord {

        private short _coordinate;
        private int _deviceTableOffset;
        
        BaseCoordFormat3(DataInput di) throws IOException {
            _coordinate = di.readShort();
            _deviceTableOffset = di.readUnsignedShort();
        }

        public int getBaseCoordFormat() {
            return 2;
        }
        
        public short getCoordinate() {
            return _coordinate;
        }
        
    }
    
    private static class FeatMinMaxRecord {
        
        private int _tag;
        private int _minCoordOffset;
        private int _maxCoordOffset;
        
        FeatMinMaxRecord(DataInput di) throws IOException {
            _tag = di.readInt();
            _minCoordOffset = di.readUnsignedShort();
            _maxCoordOffset = di.readUnsignedShort();
        }
    }
    
    private class MinMax {
        
        private int _minCoordOffset;
        private int _maxCoordOffset;
        private int _featMinMaxCount;
        private FeatMinMaxRecord[] _featMinMaxRecord;
        
        MinMax(int minMaxOffset) throws IOException {
            DataInput di = getDataInputForOffset(minMaxOffset);
            _minCoordOffset = di.readUnsignedShort();
            _maxCoordOffset = di.readUnsignedShort();
            _featMinMaxCount = di.readUnsignedShort();
            _featMinMaxRecord = new FeatMinMaxRecord[_featMinMaxCount];
            for (int i = 0; i < _featMinMaxCount; ++i) {
                _featMinMaxRecord[i] = new FeatMinMaxRecord(di);
            }
        }
    }
    
    private class BaseValues {
        
        private int _defaultIndex;
        private int _baseCoordCount;
        private int[] _baseCoordOffset;
        private BaseCoord[] _baseCoords;
        
        BaseValues(int baseValuesOffset) throws IOException {
            DataInput di = getDataInputForOffset(baseValuesOffset);
            _defaultIndex = di.readUnsignedShort();
            _baseCoordCount = di.readUnsignedShort();
            _baseCoordOffset = new int[_baseCoordCount];
            for (int i = 0; i < _baseCoordCount; ++i) {
                _baseCoordOffset[i] = di.readUnsignedShort();
            }
            _baseCoords = new BaseCoord[_baseCoordCount];
            for (int i = 0; i < _baseCoordCount; ++i) {
                int format = di.readUnsignedShort();
                switch (format) {
                    case 1:
                        _baseCoords[i] = new BaseCoordFormat1(di);
                        break;
                    case 2:
                        _baseCoords[i] = new BaseCoordFormat2(di);
                        break;
                    case 3:
                        _baseCoords[i] = new BaseCoordFormat3(di);
                        break;
                }
            }
        }
    }
    
    private static class BaseLangSysRecord {
        
        private int _baseLangSysTag;
        private int _minMaxOffset;
        
        BaseLangSysRecord(DataInput di) throws IOException {
            _baseLangSysTag = di.readInt();
            _minMaxOffset = di.readUnsignedShort();
        }

        public int getBaseLangSysTag() {
            return _baseLangSysTag;
        }
        
        int getMinMaxOffset() {
            return _minMaxOffset;
        }
    }
    
    private class BaseScript {
        
        private final int _thisOffset;
        private int _baseValuesOffset;
        private int _defaultMinMaxOffset;
        private int _baseLangSysCount;
        private BaseLangSysRecord[] _baseLangSysRecord;
        private BaseValues _baseValues;
        private MinMax[] _minMax;
        
        BaseScript(int baseScriptOffset) throws IOException {
            _thisOffset = baseScriptOffset;
            DataInput di = getDataInputForOffset(baseScriptOffset);
            _baseValuesOffset = di.readUnsignedShort();
            _defaultMinMaxOffset = di.readUnsignedShort();
            _baseLangSysCount = di.readUnsignedShort();
            _baseLangSysRecord = new BaseLangSysRecord[_baseLangSysCount];
            for (int i = 0; i < _baseLangSysCount; ++i) {
                _baseLangSysRecord[i] = new BaseLangSysRecord(di);
            }
            if (_baseValuesOffset > 0) {
                _baseValues = new BaseValues(baseScriptOffset + _baseValuesOffset);
            }
            for (int i = 0; i < _baseLangSysCount; ++i) {
                _minMax[i] = new MinMax(baseScriptOffset + _baseLangSysRecord[i].getMinMaxOffset());
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder()
                .append("\nBaseScript BaseScriptT").append(Integer.toHexString(_thisOffset))
                .append("\nBaseValuesT").append(Integer.toHexString(_thisOffset + _baseValuesOffset))
                .append("\nMinMaxT").append(Integer.toHexString(_thisOffset + _defaultMinMaxOffset))
                .append("\n").append(Integer.toHexString(_baseLangSysCount));
//            for (int i = 0; i < _baseLangSysCount; ++i) {
//                sb.append("\n                          ; BaseScriptRecord[").append(i);
//                sb.append("]\n'").append(tagAsString(_baseScriptRecord[i].getBaseScriptTag())).append("'");
//                sb.append("\nBaseScriptT").append(Integer.toHexString(_thisOffset + _baseScriptRecord[i].getBaseScriptOffset()));
//            }
//            for (int i = 0; i < _baseScriptCount; ++i) {
//                sb.append("\n").append(_baseScripts[i].toString());
//            }
            if (_baseValues != null) {
                sb.append("\n").append(_baseValues.toString());
            }
            return sb.toString();
        }
    }
    
    private static class BaseScriptRecord {
        
        private int _baseScriptTag;
        private int _baseScriptOffset;

        BaseScriptRecord(DataInput di) throws IOException {
            _baseScriptTag = di.readInt();
            _baseScriptOffset = di.readUnsignedShort();
        }

        int getBaseScriptTag() {
            return _baseScriptTag;
        }
        
        int getBaseScriptOffset() {
            return _baseScriptOffset;
        }
    }
    
    private class BaseScriptList {
        
        private final int _thisOffset;
        private int _baseScriptCount;
        private BaseScriptRecord[] _baseScriptRecord;
        private BaseScript[] _baseScripts;
 
        BaseScriptList(int baseScriptListOffset) throws IOException {
            _thisOffset = baseScriptListOffset;
            DataInput di = getDataInputForOffset(baseScriptListOffset);
            _baseScriptCount = di.readUnsignedShort();
            _baseScriptRecord = new BaseScriptRecord[_baseScriptCount];
            for (int i = 0; i < _baseScriptCount; ++i) {
                _baseScriptRecord[i] = new BaseScriptRecord(di);
            }
            _baseScripts = new BaseScript[_baseScriptCount];
            for (int i = 0; i < _baseScriptCount; ++i) {
                _baseScripts[i] = new BaseScript(
                        baseScriptListOffset + _baseScriptRecord[i].getBaseScriptOffset());
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder()
                .append("\nBaseScriptList BaseScriptListT").append(Integer.toHexString(_thisOffset))
                .append("\n").append(Integer.toHexString(_baseScriptCount));
            for (int i = 0; i < _baseScriptCount; ++i) {
                sb.append("\n                          ; BaseScriptRecord[").append(i);
                sb.append("]\n'").append(tagAsString(_baseScriptRecord[i].getBaseScriptTag())).append("'");
                sb.append("\nBaseScriptT").append(Integer.toHexString(_thisOffset + _baseScriptRecord[i].getBaseScriptOffset()));
            }
            for (int i = 0; i < _baseScriptCount; ++i) {
                sb.append("\n").append(_baseScripts[i].toString());
            }
            return sb.toString();
        }
     }
    
    private class BaseTagList {
        
        private final int _thisOffset;
        private int _baseTagCount;
        private int[] _baselineTag;
        
        BaseTagList(int baseTagListOffset) throws IOException {
            _thisOffset = baseTagListOffset;
            DataInput di = getDataInputForOffset(baseTagListOffset);
            _baseTagCount = di.readUnsignedShort();
            _baselineTag = new int[_baseTagCount];
            for (int i = 0; i < _baseTagCount; ++i) {
                _baselineTag[i] = di.readInt();
            }
        }

        public String toString() {
            StringBuilder sb = new StringBuilder()
                .append("\nBaseTagList BaseTagListT").append(Integer.toHexString(_thisOffset))
                .append("\n").append(Integer.toHexString(_baseTagCount));
            for (int i = 0; i < _baseTagCount; ++i) {
                sb.append("\n'").append(tagAsString(_baselineTag[i])).append("'");
            }
            return sb.toString();
        }
    }
    
    private class Axis {
        
        private final int _thisOffset;
        private int _baseTagListOffset;
        private int _baseScriptListOffset;
        private BaseTagList _baseTagList;
        private BaseScriptList _baseScriptList;

        Axis(int axisOffset) throws IOException {
            _thisOffset = axisOffset;
            DataInput di = getDataInputForOffset(axisOffset);
            _baseTagListOffset = di.readUnsignedShort();
            _baseScriptListOffset = di.readUnsignedShort();
            if (_baseTagListOffset != 0) {
                _baseTagList = new BaseTagList(axisOffset + _baseTagListOffset);
            }
            if (_baseScriptListOffset != 0) {
                _baseScriptList = new BaseScriptList(
                        axisOffset + _baseScriptListOffset);
            }
        }

        public String toString() {
            return "\nAxis AxisT" + Integer.toHexString(_thisOffset) +
                    "\nBaseTagListT" + Integer.toHexString(_thisOffset + _baseTagListOffset) +
                    "\nBaseScriptListT" + Integer.toHexString(_thisOffset + _baseScriptListOffset) +
                    "\n" + _baseTagList +
                    "\n" + _baseScriptList;
        }
    }
    
    private int _version;
    private int _horizAxisOffset;
    private int _vertAxisOffset;
    private Axis _horizAxis;
    private Axis _vertAxis;
    private byte[] _buf;

    /** Creates a new instance of BaseTable */
    protected BaseTable(DataInput di, int length) throws IOException {

        // Load entire table into a buffer, and create another input stream
        _buf = new byte[length];
        di.readFully(_buf);
        DataInput di2 = getDataInputForOffset(0);

        _version = di2.readInt();
        _horizAxisOffset = di2.readUnsignedShort();
        _vertAxisOffset = di2.readUnsignedShort();
        if (_horizAxisOffset != 0) {
            _horizAxis = new Axis(_horizAxisOffset);
        }
        if (_vertAxisOffset != 0) {
            _vertAxis = new Axis(_vertAxisOffset);
        }
        
        // Let go of the buffer
        _buf = null;
    }
    
    private DataInput getDataInputForOffset(int offset) {
        return new DataInputStream(new ByteArrayInputStream(
                _buf, offset,
                _buf.length - offset));
    }
    
//    private String valueAsShortHex(int value) {
//        return String.format("%1$4x", value);
//    }
//    
//    private String valueAsLongHex(int value) {
//        return String.format("%1$8x", value);
//    }
    
    private static String tagAsString(int tag) {
        char[] c = new char[4];
        c[0] = (char)((tag >> 24) & 0xff);
        c[1] = (char)((tag >> 16) & 0xff);
        c[2] = (char)((tag >> 8) & 0xff);
        c[3] = (char)(tag & 0xff);
        return String.valueOf(c);
    }
    
    public String toString() {
        StringBuilder sb = new StringBuilder()
            .append("; 'BASE' Table - Baseline\n;-------------------------------------\n\n")
            .append("BASEHeader BASEHeaderT").append(Integer.toHexString(0))
            .append("\n").append(Integer.toHexString(_version))
            .append("\nAxisT").append(Integer.toHexString(_horizAxisOffset))
            .append("\nAxisT").append(Integer.toHexString(_vertAxisOffset));
        if (_horizAxis != null) {
            sb.append("\n").append(_horizAxis.toString());
        }
        if (_vertAxis != null) {
            sb.append("\n").append(_vertAxis.toString());
        }
        return sb.toString();
    }

}
