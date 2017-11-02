#include "srs_metadata.h"

char inline *put_byte( char *out_chPut, unsigned char in_uchVal )
{
    out_chPut[0] = in_uchVal;
    return out_chPut+1;
}

char inline *put_be16(char *out_chPut, unsigned short in_usVal )
{
    out_chPut[1] = in_usVal & 0xff;
    out_chPut[0] = in_usVal >> 8;
    return out_chPut+2;
}

char inline *put_be24(char *out_chPut, unsigned short in_usVal )
{
    out_chPut[2] = in_usVal & 0xff;
    out_chPut[1] = in_usVal & 16;
    out_chPut[0] = in_usVal >> 8;
    return out_chPut+3;
}

char inline *put_be32(char *out_chPut, unsigned int in_unVal )
{
    out_chPut[3] = in_unVal & 0xff;
    out_chPut[2] = in_unVal >> 8;
    out_chPut[1] = in_unVal >> 16;
    out_chPut[0] = in_unVal >> 24;
    return out_chPut+4;
}

char inline *put_amf_string( char *out_chPut, const char *in_chBuf )
{
    uint16_t len = (uint16_t)strlen( in_chBuf );
    out_chPut=put_be16( out_chPut, len );
    memcpy(out_chPut,in_chBuf,len);
    return out_chPut+len;
}

char inline *put_amf_double( char *out_chPut, double in_dBuf )
{
    *out_chPut++ = AMF_NUMBER;    // type: Number
    {
        unsigned char *ci, *co;
        ci = (unsigned char *)&in_dBuf;
        co = (unsigned char *)out_chPut;
        co[0] = ci[7];
        co[1] = ci[6];
        co[2] = ci[5];
        co[3] = ci[4];
        co[4] = ci[3];
        co[5] = ci[2];
        co[6] = ci[1];
        co[7] = ci[0];
    }
    return out_chPut+8;
}

int srs_gen_metadata(char *flv,  bool bHasVideo, int width, int height, double videodatarate,
    double framerate, bool bHasAudio, double audiosamplerate, double audiosamplesize)
{
    char *chMetaData = (char *)flv;
    char *chMetaDataTemp = (char *)flv;

    // SCRIPTDATASTRING
    chMetaDataTemp = put_byte(chMetaDataTemp, AMF_STRING);
    chMetaDataTemp = put_amf_string(chMetaDataTemp, "onMetaData");

    // SCRIPTDATAECMAARRAY
    chMetaDataTemp = put_byte(chMetaDataTemp, AMF_ECMA_ARRAY);

    // ECMAArrayLength : UI32 ARRAY length
    chMetaDataTemp = put_be32(chMetaDataTemp, 21);

    // PropertyName
    chMetaDataTemp = put_amf_string(chMetaDataTemp, "MetaDataCreator");
    // Type:UI8 datatype
    chMetaDataTemp = put_byte(chMetaDataTemp, AMF_STRING);
    // ScriptDataValue: data
    chMetaDataTemp = put_amf_string(chMetaDataTemp, "created by xxx @2017");

    // PropertyName
    chMetaDataTemp = put_amf_string(chMetaDataTemp, "hasKeyFrames");
    // Type:UI8 datatype
    chMetaDataTemp = put_byte(chMetaDataTemp, AMF_BOOLEAN);
    // ScriptDataValue: data
    chMetaDataTemp = put_byte(chMetaDataTemp, 0);

    chMetaDataTemp = put_amf_string(chMetaDataTemp, "hasVideo");
    chMetaDataTemp = put_byte(chMetaDataTemp, AMF_BOOLEAN);
    chMetaDataTemp = put_byte(chMetaDataTemp, bHasVideo ? 1 : 0);

    chMetaDataTemp = put_amf_string(chMetaDataTemp, "hasAudio");
    chMetaDataTemp = put_byte(chMetaDataTemp, AMF_BOOLEAN);
    chMetaDataTemp = put_byte(chMetaDataTemp, bHasAudio ? 1 : 0);

    chMetaDataTemp = put_amf_string(chMetaDataTemp, "hasMatadata");
    chMetaDataTemp = put_byte(chMetaDataTemp, AMF_BOOLEAN);
    chMetaDataTemp = put_byte(chMetaDataTemp, 1);

    chMetaDataTemp = put_amf_string(chMetaDataTemp, "canSeekToEnd");
    chMetaDataTemp = put_byte(chMetaDataTemp, AMF_BOOLEAN);
    chMetaDataTemp = put_byte(chMetaDataTemp, 0);

    // PropertyName
    chMetaDataTemp = put_amf_string( chMetaDataTemp, "duration");
    // Type:UI8 datatype + ScriptDataValue: data
    chMetaDataTemp = put_amf_double( chMetaDataTemp, (double)0.0);

    chMetaDataTemp = put_amf_string( chMetaDataTemp, "width");
    chMetaDataTemp = put_amf_double( chMetaDataTemp, (double)width);

    chMetaDataTemp = put_amf_string( chMetaDataTemp, "height");
    chMetaDataTemp = put_amf_double( chMetaDataTemp, (double)height);

    chMetaDataTemp = put_amf_string( chMetaDataTemp, "videodatarate");
    chMetaDataTemp = put_amf_double( chMetaDataTemp, (double)videodatarate);

    chMetaDataTemp = put_amf_string( chMetaDataTemp, "framerate");
    chMetaDataTemp = put_amf_double( chMetaDataTemp, (double)framerate);

    chMetaDataTemp = put_amf_string( chMetaDataTemp, "videocodecid");
    chMetaDataTemp = put_amf_double( chMetaDataTemp, (double)7.0);

    chMetaDataTemp = put_amf_string( chMetaDataTemp, "audiosamplerate");
    chMetaDataTemp = put_amf_double( chMetaDataTemp, (double)audiosamplerate);

    chMetaDataTemp = put_amf_string( chMetaDataTemp, "audiosamplesize");
    chMetaDataTemp = put_amf_double( chMetaDataTemp, (double)audiosamplesize);

    chMetaDataTemp = put_amf_string(chMetaDataTemp, "stereo");
    chMetaDataTemp = put_byte(chMetaDataTemp, AMF_BOOLEAN);
    chMetaDataTemp = put_byte(chMetaDataTemp, 1);

    chMetaDataTemp = put_amf_string( chMetaDataTemp, "audiocodecid");
    chMetaDataTemp = put_amf_double( chMetaDataTemp, (double)10.0);

    chMetaDataTemp = put_amf_string( chMetaDataTemp, "filesize");
    chMetaDataTemp = put_amf_double( chMetaDataTemp, (double)0.0);

    // List Terminator: SCRIPTDATAOBJECTEND
    // SCRIPTDATAOBJECTEND : 0x00 0x00 0x09
    chMetaDataTemp = put_be24( chMetaDataTemp, 9);

    return chMetaDataTemp -chMetaData;
}
