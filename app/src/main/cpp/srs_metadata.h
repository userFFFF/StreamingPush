#include "srs_librtmp.h"

enum MFDataType
{
    AMF_NUMBER = 0, AMF_BOOLEAN, AMF_STRING, AMF_OBJECT,
    AMF_MOVIECLIP,        /* reserved, not used */
    AMF_NULL, AMF_UNDEFINED, AMF_REFERENCE, AMF_ECMA_ARRAY, AMF_OBJECT_END,
    AMF_STRICT_ARRAY, AMF_DATE, AMF_LONG_STRING, AMF_UNSUPPORTED,
    AMF_RECORDSET,        /* reserved, not used */
    AMF_XML_DOC, AMF_TYPED_OBJECT,
    AMF_AVMPLUS,        /* switch to AMF3 */
    AMF_INVALID = 0xff
};

extern int srs_gen_metadata(char *flv,  bool bHasVideo, int width, int height, double videodatarate,
    double framerate, bool bHasAudio, double audiosamplerate, double audiosamplesize);
