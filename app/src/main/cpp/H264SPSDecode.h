//
// Created by USR0 on 2017/11/17.
//

#ifndef STREAMINGPUSH_H264SPSDECODE_H
#define STREAMINGPUSH_H264SPSDECODE_H

extern int h264_get_sps(char *buf, int size);

extern bool
h264_decode_sps(unsigned char *buf, unsigned int nLen, int &width, int &height, int &fps);

#endif //STREAMINGPUSH_H264SPSDECODE_H
