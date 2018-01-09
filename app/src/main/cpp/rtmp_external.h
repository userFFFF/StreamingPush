#ifndef RTMP_EXTERNAL_HPP
#define RTMP_EXTERNAL_HPP

#include <stdint.h>
#include <sys/types.h>

#ifdef __cplusplus
extern "C"{
#endif

typedef void* rtmp_t;

#define rtmp_stopped             2
#define rtmp_connected           1
#define rtmp_success             0
#define rtmp_create_error        -1
#define rtmp_handshake_error     -2
#define rtmp_connect_error       -3
#define rtmp_stream_error        -4
#define rtmp_stream_unsupport    -5
#define rtmp_handle_error        -6
#define rtmp_stream_size_error   -7
#define rtmp_socket_error        1019

/**
* rtmp_create:
*
* Create a RTMP handler.
* @param url The RTMP url, for example, rtmp://localhost/live/livestream
*
* @return a rtmp handler, or NULL if error occured.
*/
extern rtmp_t rtmp_create(const char *pUrl);

/**
* rtmp_connect:
*
* Connect invoke:
* @param rtmp : RTMP Handle
* @handshake with server
* @connect to RTMP tcUrl(Vhost/App)
* @publish a live stream
* category: publish
* previous: rtmp_create
* next: rtmp_push_scriptdata
*
* @return 0, success; otherswise, failed.
*/
extern int rtmp_connect(rtmp_t rtmp);

/**
* rtmp_push_scriptdata:
*
* @Generate metadata based on audio and video parameters
* @as SCIPTDATA TAG for FLV
* category: publish
* previous: rtmp_connect
* next: rtmp_push_video/rtmp_push_audio
*
* @return 0, success; otherswise, failed.
*/
extern int rtmp_push_scriptdata(rtmp_t rtmp,
    bool bHasVideo, int width, int height, double videodatarate, double framerate,
    bool bHasAudio, double audiosamplerate, double audiosamplesize);

/**
* rtmp_push_video:
*
* @param rtmp : RTMP Handle
* @param framedata : h264 raw data
* @param frames_size the size of h264 raw data.
* @param dts the dts of h.264 raw data.
* @param pts the pts of h.264 raw data.
* category: publish
* previous: rtmp_push_scriptdata
* next: rtmp_stop
*
* @return 0, success; otherswise, failed.
*
*/
/**
For example :
The data sequence is:
    // SPS
    000000016742802995A014016E40
    // PPS
    0000000168CE3880
    // Frame
    0000000165B8041014C038008B0D0D3A071.....
User can send the SPS+PPS, then each frame:
    // SPS+PPS
    rtmp_push_video(rtmp, '000000016742802995A014016E400000000168CE3880', size, dts, pts)
    // Frame
    rtmp_push_video(rtmp, '0000000165B8041014C038008B0D0D3A071......', size, dts, pts)
User also can send one by one:
    // SPS
    rtmp_push_video(rtmp, '000000016742802995A014016E4', size, dts, pts)
    // PPS
    rtmp_push_video(rtmp, '00000000168CE3880', size, dts, pts)
    // Frame
    rtmp_push_video(rtmp, '0000000165B8041014C038008B0D0D3A071......', size, dts, pts)
*/
extern int rtmp_push_video(rtmp_t rtmp,
    char* framedata, int framesize, uint32_t dts,uint32_t pts);

/**
* rtmp_push_audio:
*
* @param sound_format Format of SoundData. The following values are defined:
*               0 = Linear PCM, platform endian
*               1 = ADPCM
*               2 = MP3
*               3 = Linear PCM, little endian
*               4 = Nellymoser 16 kHz mono
*               5 = Nellymoser 8 kHz mono
*               6 = Nellymoser
*               7 = G.711 A-law logarithmic PCM
*               8 = G.711 mu-law logarithmic PCM
*               9 = reserved
*               10 = AAC
*               11 = Speex
*               14 = MP3 8 kHz
*               15 = Device-specific sound
*               Formats 7, 8, 14, and 15 are reserved.
*               AAC is supported in Flash Player 9,0,115,0 and higher.
*               Speex is supported in Flash Player 10 and higher.
* @param sound_rate Sampling rate. The following values are defined:
*               0 = 5.5 kHz
*               1 = 11 kHz
*               2 = 22 kHz
*               3 = 44 kHz
* @param sound_size Size of each audio sample. This parameter only pertains to
*               uncompressed formats. Compressed formats always decode
*               to 16 bits internally.
*               0 = 8-bit samples
*               1 = 16-bit samples
* @param sound_type Mono or stereo sound
*               0 = Mono sound
*               1 = Stereo sound
* @param framedata : AAC raw data + ADTS Header
* @param frames_size the size of AAC raw data  + ADTS Header
* @param timestamp The timestamp of audio.
*
* @return 0, success; otherswise, failed.
*/
extern int rtmp_push_audio(rtmp_t rtmp,
char sound_format, char sound_rate, char sound_size, char sound_type,
char* framedata, int framesize, uint32_t timestamp);

/**
* rtmp_stop:
*
* close and destroy the rtmp stack.
* @remark, user should never use the rtmp again.
*
* @return 0, success; otherswise, failed.
*/
extern void rtmp_stop(rtmp_t rtmp);

#ifdef __cplusplus
}
#endif

#endif

