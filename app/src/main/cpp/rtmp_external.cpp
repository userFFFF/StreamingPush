#include "srs_librtmp.h"
#include "rtmp_external.h"

rtmp_t rtmp_create(const char *pUrl)
{
    return srs_rtmp_create(pUrl);
}

int rtmp_connect(rtmp_t rtmp)
{
    int ret = rtmp_success;
    if((ret = srs_rtmp_handshake((srs_rtmp_t *)rtmp)) != rtmp_success)
    {
        return rtmp_handshake_error;
    }
    if((ret = srs_rtmp_connect_app((srs_rtmp_t *)rtmp)) != rtmp_success)
    {
        return rtmp_connect_error;
    }
    if((ret = srs_rtmp_publish_stream((srs_rtmp_t *)rtmp)) != rtmp_success)
    {
        return rtmp_stream_error;
    }
    return rtmp_success;
}

int rtmp_push_scriptdata(rtmp_t rtmp, bool bHasVideo, 
    int width, int height, double videodatarate, double framerate,
    bool bHasAudio, double audiosamplerate, double audiosamplesize)
{
    return srs_write_scriptdata((srs_rtmp_t *)rtmp, bHasVideo, width, height, videodatarate, framerate, 
        bHasAudio, audiosamplerate, audiosamplesize);
}

int rtmp_push_video(rtmp_t rtmp,
    char* framedata, int framesize, uint32_t dts,uint32_t pts)
{
    return srs_h264_write_raw_frames((srs_rtmp_t *)rtmp, framedata, 
        framesize, dts, pts);
}

int rtmp_push_audio(rtmp_t rtmp, char sound_format,
    char sound_rate, char sound_size, char sound_type,
    char* framedata, int framesize, uint32_t timestamp)
{
    return srs_audio_write_raw_frame((srs_rtmp_t *)rtmp,
        sound_format, sound_rate, sound_size, sound_type,
        framedata, framesize, timestamp);
}

void rtmp_stop(rtmp_t rtmp)
{
    if(rtmp != NULL)
    {
        srs_rtmp_destroy((srs_rtmp_t *)rtmp);
    }
}

