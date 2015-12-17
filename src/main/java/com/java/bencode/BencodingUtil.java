package com.java.bencode;

import java.nio.ByteBuffer;

enum BencodeState {
    NULL_STATE,
    INTEGER_STATE,
    STRING_LENGTH_STATE,
    STRING_CONTENT_STATE,
    LIST_STATE,
    DICTIONARY_STATE
}

class Result {
    byte[] byteArr;
    int index;

    public Result(byte[] byteArr, int index) {
        this.byteArr = byteArr;
        this.index = index;
    }
}

@SuppressWarnings("incomplete-switch")
public class BencodingUtil {
    public static byte[] encodeBytes(byte[] data) {
        return encode(data, 0).byteArr;
    }

    public static byte[] decodeBytes(byte[] data) {
        return decode(data, 0).byteArr;
    }

    private static Result encode(byte[] data, int index) {
        int idx = index;
        BencodeState state = BencodeState.NULL_STATE;
        byte[] current = new byte[0];
        int stringLength = 0;
        int arrayLength = data.length;

        while (idx < arrayLength) {
            byte c = data[idx];

            switch (state) {
            case NULL_STATE:
                if (c == '"') {
                    state = BencodeState.STRING_CONTENT_STATE;
                    current = new byte[0];
                } else if (c >= '0' && c <= '9') {
                    if (idx > 0 && data[idx - 1] != '"') {
                        state = BencodeState.INTEGER_STATE;
                        current = appendByte(new byte[0], c);
                    }
                } else if (c == '[') {
                    current = appendBytes(current, "l".getBytes());
                    state = BencodeState.LIST_STATE;
                } else if (c == '{') {
                    current = appendBytes(current, "d".getBytes());
                    state = BencodeState.DICTIONARY_STATE;
                } else if (c == ':' || c == ',') {
                    ++idx;
                    break;
                } else {
                    return new Result(new byte[0], idx + 1);
                }

                ++idx;
                break;

            case STRING_CONTENT_STATE:
                if (c == '"' && isEndOfString(data, idx)) {
                    byte[] stringPrefix = (String.valueOf(stringLength) + ":").getBytes();
                    current = appendBytes(stringPrefix, current);
                    return new Result(current, idx + 1);
                } else {
                    ++stringLength;
                    current = appendByte(current, c);
                }

                ++idx;
                break;

            case INTEGER_STATE:
                if (c < '0' || c > '9') {
                    current = appendBytes("i".getBytes(), current);
                    current = appendBytes(current, "e".getBytes());
                    return new Result(current, idx + 1);
                } else {
                    current = appendByte(current, c);
                }

                ++idx;
                break;

            case DICTIONARY_STATE:
                if (c == '}') {
                    current = appendBytes(current, "e".getBytes());
                    return new Result(current, idx + 1);
                } else {
                    Result key = encode(data, idx);
                    Result value = encode(data, key.index);
                    current = appendBytes(current, key.byteArr);
                    current = appendBytes(current, value.byteArr);
                    idx = value.index;
                }

                break;

            case LIST_STATE:
                if (c == ']') {
                    current = appendBytes(current, "e".getBytes());
                    return new Result(current, idx + 1);
                } else {
                    Result res = encode(data, idx);
                    current = appendBytes(current, res.byteArr);
                    idx = res.index;
                }

                break;
            }
        }

        return new Result(current, idx + 1);
    }

    private static Result decode(byte[] data, int index) {
        int idx = index;
        BencodeState state = BencodeState.NULL_STATE;
        byte[] current = new byte[0];
        String stringLengthStr = "";
        int stringLength = 0;
        int arrayLength = data.length;
        ByteBuffer bf = null;

        while (idx < arrayLength) {
            byte c = data[idx];

            switch (state) {
            case NULL_STATE:
                if (c == 'i') {
                    state = BencodeState.INTEGER_STATE;
                    current = new byte[0];
                } else if (c >= '0' && c <= '9') {
                    state = BencodeState.STRING_LENGTH_STATE;
                    stringLengthStr = "" + (char) c;
                } else if (c == 'l') {
                    current = appendBytes(current, "[".getBytes());
                    state = BencodeState.LIST_STATE;
                } else if (c == 'd') {
                    current = appendBytes(current, "{".getBytes());
                    state = BencodeState.DICTIONARY_STATE;
                } else {
                    return new Result(new byte[0], idx + 1);
                }

                ++idx;
                break;

            case INTEGER_STATE:
                if (c == '-') {
                    current = appendBytes(current, "-".getBytes());
                } else if (c == 'e') {
                    return new Result(current, idx + 1);
                } else if (c >= '0' && c <= '9') {
                    current = appendByte(current, c);
                }

                ++idx;
                break;

            case STRING_LENGTH_STATE:
                if (c == ':') {
                    stringLength = Integer.parseInt(stringLengthStr);
                    state = BencodeState.STRING_CONTENT_STATE;
                    bf = ByteBuffer.allocate(stringLength);
                    current = new byte[0];
                } else if (c >= '0' && c <= '9') {
                    stringLengthStr += (char) c;
                } else {
                    return new Result(new byte[0], idx + 1);
                }

                ++idx;
                break;

            case STRING_CONTENT_STATE:
                bf.put(c);
                if (--stringLength == 0) {
                    byte[] qArr = "\"".getBytes();
                    current = appendBytes(qArr, bf.array());
                    current = appendBytes(current, qArr);
                    return new Result(current, idx + 1);
                }

                ++idx;
                break;

            case DICTIONARY_STATE:
                if (c == 'e') {
                    current = appendBytes(current, "}".getBytes());
                    return new Result(current, idx + 1);
                } else {
                    Result key = decode(data, idx);
                    Result value = decode(data, key.index);
                    current = appendBytes(current, key.byteArr);
                    current = appendBytes(current, ":".getBytes());
                    current = appendBytes(current, value.byteArr);
                    idx = value.index;
                    if (idx < arrayLength && data[idx] != 'e') {
                        current = appendBytes(current, ",".getBytes());
                    }
                }

                break;

            case LIST_STATE:
                if (c == 'e') {
                    current = appendBytes(current, "]".getBytes());
                    return new Result(current, idx + 1);
                } else {
                    Result res = decode(data, idx);
                    current = appendBytes(current, res.byteArr);
                    idx = res.index;
                    if (idx < arrayLength && data[idx] != 'e') {
                        current = appendBytes(current, ",".getBytes());
                    }
                }

                break;
            }
        }

        return new Result(current, idx + 1);
    }

    private static byte[] appendByte(byte[] oldArr, byte b) {
        byte[] newArr = new byte[oldArr.length + 1];
        for (int i = 0; i < oldArr.length; i++) {
            newArr[i] = oldArr[i];
        }

        newArr[oldArr.length] = b;
        return newArr;
    }

    private static byte[] appendBytes(byte[] oldArr, byte[] bArr) {
        int newLength = oldArr.length + bArr.length;
        byte[] newArr = new byte[newLength];
        for (int i = 0; i < oldArr.length; i++) {
            newArr[i] = oldArr[i];
        }

        for (int i = oldArr.length; i < newLength; i++) {
            newArr[i] = bArr[i - oldArr.length];
        }
        return newArr;
    }

    private static boolean isEndOfString(byte[] data, int index) {
        int length = data.length;
        if (index == length - 1) {
            return true;
        }

        if (data[index + 1] == ':' || data[index + 1] == ',' || data[index + 1] == ']' || data[index + 1] == '}') {
            if (index + 2 < length && !isPrintableChar(data[index + 2])) {
                return false;
            }
            if (index + 3 < length && !isPrintableChar(data[index + 3])) {
                return false;
            }
            if (index + 4 < length && !isPrintableChar(data[index + 4])) {
                return false;
            }
            if (index + 5 < length && !isPrintableChar(data[index + 5])) {
                return false;
            }

            return true;
        }

        return false;
    }

    private static boolean isPrintableChar(byte b) {
        if (b < 32 || b > 126) {
            return false;
        } else {
            return true;
        }
    }
}
