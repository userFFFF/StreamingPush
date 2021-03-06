#include "srs_librtmp.h"
#include "H264SPSDecode.h"
#include "rtmp_external.h"

rtmp_t rtmp_create(const char *pUrl) {
    return srs_rtmp_create(pUrl);
}

int rtmp_connect(rtmp_t rtmp) {
    if (srs_rtmp_handshake((srs_rtmp_t *) rtmp) != rtmp_success) {
        return rtmp_handshake_error;
    }
    if (srs_rtmp_connect_app((srs_rtmp_t *) rtmp) != rtmp_success) {
        return rtmp_connect_error;
    }
    if (srs_rtmp_publish_stream((srs_rtmp_t *) rtmp) != rtmp_success) {
        return rtmp_stream_error;
    }
    return rtmp_connected;
}

int rtmp_push_scriptdata(rtmp_t rtmp, bool bHasVideo,
                         int width, int height, double videodatarate, double framerate,
                         bool bHasAudio, double audiosamplerate, double audiosamplesize) {
    return srs_write_scriptdata((srs_rtmp_t *) rtmp, bHasVideo, width, height, videodatarate,
                                framerate,
                                bHasAudio, audiosamplerate, audiosamplesize);
}

int rtmp_push_video(rtmp_t rtmp,
                    char *framedata, int framesize, uint32_t dts, uint32_t pts) {
    static bool once = false;
    if ((framedata[4] & 0x1F == 7) && !once) {// onMetadata
        int width, height, fps, nSPSsize;
        once = true;
        nSPSsize = h264_get_sps(framedata, framesize);
        h264_decode_sps((unsigned char *)framedata, nSPSsize, width, height, fps);
        rtmp_push_scriptdata((srs_rtmp_t *) rtmp, true, width, height, 0, 20, false, 0, 0);
    }
    if(!once){
        once = true;
        rtmp_push_scriptdata((srs_rtmp_t *) rtmp, true, 640, 480, 0, 20, false, 0, 0);
    }
    return srs_h264_write_raw_frames((srs_rtmp_t *) rtmp, framedata,
                                     framesize, dts, pts);
}

int rtmp_push_audio(rtmp_t rtmp, char sound_format,
                    char sound_rate, char sound_size, char sound_type,
                    char *framedata, int framesize, uint32_t timestamp) {
    return srs_audio_write_raw_frame((srs_rtmp_t *) rtmp,
                                     sound_format, sound_rate, sound_size, sound_type,
                                     framedata, framesize, timestamp);
}

void rtmp_stop(rtmp_t rtmp) {
    if (rtmp != NULL) {
        srs_rtmp_destroy((srs_rtmp_t *) rtmp);
    }
}