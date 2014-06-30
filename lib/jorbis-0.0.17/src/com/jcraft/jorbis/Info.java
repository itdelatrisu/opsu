/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/* JOrbis
 * Copyright (C) 2000 ymnk, JCraft,Inc.
 *  
 * Written by: 2000 ymnk<ymnk@jcraft.com>
 *   
 * Many thanks to 
 *   Monty <monty@xiph.org> and 
 *   The XIPHOPHORUS Company http://www.xiph.org/ .
 * JOrbis has been based on their awesome works, Vorbis codec.
 *   
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Library General Public License
 * as published by the Free Software Foundation; either version 2 of
 * the License, or (at your option) any later version.
   
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 * 
 * You should have received a copy of the GNU Library General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */

package com.jcraft.jorbis;

import com.jcraft.jogg.*;

public class Info{
  private static final int OV_EBADPACKET=-136;
  private static final int OV_ENOTAUDIO=-135;

  private static byte[] _vorbis="vorbis".getBytes();
  private static final int VI_TIMEB=1;
  //  private static final int VI_FLOORB=1;
  private static final int VI_FLOORB=2;
  //  private static final int VI_RESB=1;
  private static final int VI_RESB=3;
  private static final int VI_MAPB=1;
  private static final int VI_WINDOWB=1;

  public int version;
  public int channels;
  public int rate;

  // The below bitrate declarations are *hints*.
  // Combinations of the three values carry the following implications:
  //     
  // all three set to the same value: 
  // implies a fixed rate bitstream
  // only nominal set: 
  // implies a VBR stream that averages the nominal bitrate.  No hard 
  // upper/lower limit
  // upper and or lower set: 
  // implies a VBR bitstream that obeys the bitrate limits. nominal 
  // may also be set to give a nominal rate.
  // none set:
  //  the coder does not care to speculate.

  int bitrate_upper;
  int bitrate_nominal;
  int bitrate_lower;

  // Vorbis supports only short and long blocks, but allows the
  // encoder to choose the sizes

  int[] blocksizes=new int[2];

  // modes are the primary means of supporting on-the-fly different
  // blocksizes, different channel mappings (LR or mid-side),
  // different residue backends, etc.  Each mode consists of a
  // blocksize flag and a mapping (along with the mapping setup

  int modes;
  int maps;
  int times;
  int floors;
  int residues;
  int books;
  int psys; // encode only

  InfoMode[] mode_param=null;

  int[] map_type=null;
  Object[] map_param=null;

  int[] time_type=null;
  Object[] time_param=null;

  int[] floor_type=null;
  Object[] floor_param=null;

  int[] residue_type=null;
  Object[] residue_param=null;

  StaticCodeBook[] book_param=null;

  PsyInfo[] psy_param=new PsyInfo[64]; // encode only

  // for block long/sort tuning; encode only
  int envelopesa;
  float preecho_thresh;
  float preecho_clamp;

  // used by synthesis, which has a full, alloced vi
  public void init(){
    rate=0;
  }

  public void clear(){
    for(int i=0; i<modes; i++){
      mode_param[i]=null;
    }
    mode_param=null;

    for(int i=0; i<maps; i++){ // unpack does the range checking
      FuncMapping.mapping_P[map_type[i]].free_info(map_param[i]);
    }
    map_param=null;

    for(int i=0; i<times; i++){ // unpack does the range checking
      FuncTime.time_P[time_type[i]].free_info(time_param[i]);
    }
    time_param=null;

    for(int i=0; i<floors; i++){ // unpack does the range checking
      FuncFloor.floor_P[floor_type[i]].free_info(floor_param[i]);
    }
    floor_param=null;

    for(int i=0; i<residues; i++){ // unpack does the range checking
      FuncResidue.residue_P[residue_type[i]].free_info(residue_param[i]);
    }
    residue_param=null;

    // the static codebooks *are* freed if you call info_clear, because
    // decode side does alloc a 'static' codebook. Calling clear on the
    // full codebook does not clear the static codebook (that's our
    // responsibility)
    for(int i=0; i<books; i++){
      // just in case the decoder pre-cleared to save space
      if(book_param[i]!=null){
        book_param[i].clear();
        book_param[i]=null;
      }
    }
    //if(vi->book_param)free(vi->book_param);
    book_param=null;

    for(int i=0; i<psys; i++){
      psy_param[i].free();
    }

  }

  // Header packing/unpacking
  int unpack_info(Buffer opb){
    version=opb.read(32);
    if(version!=0)
      return (-1);

    channels=opb.read(8);
    rate=opb.read(32);

    bitrate_upper=opb.read(32);
    bitrate_nominal=opb.read(32);
    bitrate_lower=opb.read(32);

    blocksizes[0]=1<<opb.read(4);
    blocksizes[1]=1<<opb.read(4);

    if((rate<1)||(channels<1)||(blocksizes[0]<8)||(blocksizes[1]<blocksizes[0])
        ||(opb.read(1)!=1)){
      clear();
      return (-1);
    }
    return (0);
  }

  // all of the real encoding details are here.  The modes, books,
  // everything
  int unpack_books(Buffer opb){

    books=opb.read(8)+1;

    if(book_param==null||book_param.length!=books)
      book_param=new StaticCodeBook[books];
    for(int i=0; i<books; i++){
      book_param[i]=new StaticCodeBook();
      if(book_param[i].unpack(opb)!=0){
        clear();
        return (-1);
      }
    }

    // time backend settings
    times=opb.read(6)+1;
    if(time_type==null||time_type.length!=times)
      time_type=new int[times];
    if(time_param==null||time_param.length!=times)
      time_param=new Object[times];
    for(int i=0; i<times; i++){
      time_type[i]=opb.read(16);
      if(time_type[i]<0||time_type[i]>=VI_TIMEB){
        clear();
        return (-1);
      }
      time_param[i]=FuncTime.time_P[time_type[i]].unpack(this, opb);
      if(time_param[i]==null){
        clear();
        return (-1);
      }
    }

    // floor backend settings
    floors=opb.read(6)+1;
    if(floor_type==null||floor_type.length!=floors)
      floor_type=new int[floors];
    if(floor_param==null||floor_param.length!=floors)
      floor_param=new Object[floors];

    for(int i=0; i<floors; i++){
      floor_type[i]=opb.read(16);
      if(floor_type[i]<0||floor_type[i]>=VI_FLOORB){
        clear();
        return (-1);
      }

      floor_param[i]=FuncFloor.floor_P[floor_type[i]].unpack(this, opb);
      if(floor_param[i]==null){
        clear();
        return (-1);
      }
    }

    // residue backend settings
    residues=opb.read(6)+1;

    if(residue_type==null||residue_type.length!=residues)
      residue_type=new int[residues];

    if(residue_param==null||residue_param.length!=residues)
      residue_param=new Object[residues];

    for(int i=0; i<residues; i++){
      residue_type[i]=opb.read(16);
      if(residue_type[i]<0||residue_type[i]>=VI_RESB){
        clear();
        return (-1);
      }
      residue_param[i]=FuncResidue.residue_P[residue_type[i]].unpack(this, opb);
      if(residue_param[i]==null){
        clear();
        return (-1);
      }
    }

    // map backend settings
    maps=opb.read(6)+1;
    if(map_type==null||map_type.length!=maps)
      map_type=new int[maps];
    if(map_param==null||map_param.length!=maps)
      map_param=new Object[maps];
    for(int i=0; i<maps; i++){
      map_type[i]=opb.read(16);
      if(map_type[i]<0||map_type[i]>=VI_MAPB){
        clear();
        return (-1);
      }
      map_param[i]=FuncMapping.mapping_P[map_type[i]].unpack(this, opb);
      if(map_param[i]==null){
        clear();
        return (-1);
      }
    }

    // mode settings
    modes=opb.read(6)+1;
    if(mode_param==null||mode_param.length!=modes)
      mode_param=new InfoMode[modes];
    for(int i=0; i<modes; i++){
      mode_param[i]=new InfoMode();
      mode_param[i].blockflag=opb.read(1);
      mode_param[i].windowtype=opb.read(16);
      mode_param[i].transformtype=opb.read(16);
      mode_param[i].mapping=opb.read(8);

      if((mode_param[i].windowtype>=VI_WINDOWB)
          ||(mode_param[i].transformtype>=VI_WINDOWB)
          ||(mode_param[i].mapping>=maps)){
        clear();
        return (-1);
      }
    }

    if(opb.read(1)!=1){
      clear();
      return (-1);
    }

    return (0);
  }

  // The Vorbis header is in three packets; the initial small packet in
  // the first page that identifies basic parameters, a second packet
  // with bitstream comments and a third packet that holds the
  // codebook.

  public int synthesis_headerin(Comment vc, Packet op){
    Buffer opb=new Buffer();

    if(op!=null){
      opb.readinit(op.packet_base, op.packet, op.bytes);

      // Which of the three types of header is this?
      // Also verify header-ness, vorbis
      {
        byte[] buffer=new byte[6];
        int packtype=opb.read(8);
        opb.read(buffer, 6);
        if(buffer[0]!='v'||buffer[1]!='o'||buffer[2]!='r'||buffer[3]!='b'
            ||buffer[4]!='i'||buffer[5]!='s'){
          // not a vorbis header
          return (-1);
        }
        switch(packtype){
          case 0x01: // least significant *bit* is read first
            if(op.b_o_s==0){
              // Not the initial packet
              return (-1);
            }
            if(rate!=0){
              // previously initialized info header
              return (-1);
            }
            return (unpack_info(opb));
          case 0x03: // least significant *bit* is read first
            if(rate==0){
              // um... we didn't get the initial header
              return (-1);
            }
            return (vc.unpack(opb));
          case 0x05: // least significant *bit* is read first
            if(rate==0||vc.vendor==null){
              // um... we didn;t get the initial header or comments yet
              return (-1);
            }
            return (unpack_books(opb));
          default:
            // Not a valid vorbis header type
            //return(-1);
            break;
        }
      }
    }
    return (-1);
  }

  // pack side
  int pack_info(Buffer opb){
    // preamble
    opb.write(0x01, 8);
    opb.write(_vorbis);

    // basic information about the stream
    opb.write(0x00, 32);
    opb.write(channels, 8);
    opb.write(rate, 32);

    opb.write(bitrate_upper, 32);
    opb.write(bitrate_nominal, 32);
    opb.write(bitrate_lower, 32);

    opb.write(Util.ilog2(blocksizes[0]), 4);
    opb.write(Util.ilog2(blocksizes[1]), 4);
    opb.write(1, 1);
    return (0);
  }

  int pack_books(Buffer opb){
    opb.write(0x05, 8);
    opb.write(_vorbis);

    // books
    opb.write(books-1, 8);
    for(int i=0; i<books; i++){
      if(book_param[i].pack(opb)!=0){
        //goto err_out;
        return (-1);
      }
    }

    // times
    opb.write(times-1, 6);
    for(int i=0; i<times; i++){
      opb.write(time_type[i], 16);
      FuncTime.time_P[time_type[i]].pack(this.time_param[i], opb);
    }

    // floors
    opb.write(floors-1, 6);
    for(int i=0; i<floors; i++){
      opb.write(floor_type[i], 16);
      FuncFloor.floor_P[floor_type[i]].pack(floor_param[i], opb);
    }

    // residues
    opb.write(residues-1, 6);
    for(int i=0; i<residues; i++){
      opb.write(residue_type[i], 16);
      FuncResidue.residue_P[residue_type[i]].pack(residue_param[i], opb);
    }

    // maps
    opb.write(maps-1, 6);
    for(int i=0; i<maps; i++){
      opb.write(map_type[i], 16);
      FuncMapping.mapping_P[map_type[i]].pack(this, map_param[i], opb);
    }

    // modes
    opb.write(modes-1, 6);
    for(int i=0; i<modes; i++){
      opb.write(mode_param[i].blockflag, 1);
      opb.write(mode_param[i].windowtype, 16);
      opb.write(mode_param[i].transformtype, 16);
      opb.write(mode_param[i].mapping, 8);
    }
    opb.write(1, 1);
    return (0);
  }

  public int blocksize(Packet op){
    //codec_setup_info
    Buffer opb=new Buffer();

    int mode;

    opb.readinit(op.packet_base, op.packet, op.bytes);

    /* Check the packet type */
    if(opb.read(1)!=0){
      /* Oops.  This is not an audio data packet */
      return (OV_ENOTAUDIO);
    }
    {
      int modebits=0;
      int v=modes;
      while(v>1){
        modebits++;
        v>>>=1;
      }

      /* read our mode and pre/post windowsize */
      mode=opb.read(modebits);
    }
    if(mode==-1)
      return (OV_EBADPACKET);
    return (blocksizes[mode_param[mode].blockflag]);
  }

  public String toString(){
    return "version:"+new Integer(version)+", channels:"+new Integer(channels)
        +", rate:"+new Integer(rate)+", bitrate:"+new Integer(bitrate_upper)
        +","+new Integer(bitrate_nominal)+","+new Integer(bitrate_lower);
  }
}
