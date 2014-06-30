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

// the comments are not part of vorbis_info so that vorbis_info can be
// static storage
public class Comment{
  private static byte[] _vorbis="vorbis".getBytes();
  private static byte[] _vendor="Xiphophorus libVorbis I 20000508".getBytes();

  private static final int OV_EIMPL=-130;

  // unlimited user comment fields.
  public byte[][] user_comments;
  public int[] comment_lengths;
  public int comments;
  public byte[] vendor;

  public void init(){
    user_comments=null;
    comments=0;
    vendor=null;
  }

  public void add(String comment){
    add(comment.getBytes());
  }

  private void add(byte[] comment){
    byte[][] foo=new byte[comments+2][];
    if(user_comments!=null){
      System.arraycopy(user_comments, 0, foo, 0, comments);
    }
    user_comments=foo;

    int[] goo=new int[comments+2];
    if(comment_lengths!=null){
      System.arraycopy(comment_lengths, 0, goo, 0, comments);
    }
    comment_lengths=goo;

    byte[] bar=new byte[comment.length+1];
    System.arraycopy(comment, 0, bar, 0, comment.length);
    user_comments[comments]=bar;
    comment_lengths[comments]=comment.length;
    comments++;
    user_comments[comments]=null;
  }

  public void add_tag(String tag, String contents){
    if(contents==null)
      contents="";
    add(tag+"="+contents);
  }

  static boolean tagcompare(byte[] s1, byte[] s2, int n){
    int c=0;
    byte u1, u2;
    while(c<n){
      u1=s1[c];
      u2=s2[c];
      if('Z'>=u1&&u1>='A')
        u1=(byte)(u1-'A'+'a');
      if('Z'>=u2&&u2>='A')
        u2=(byte)(u2-'A'+'a');
      if(u1!=u2){
        return false;
      }
      c++;
    }
    return true;
  }

  public String query(String tag){
    return query(tag, 0);
  }

  public String query(String tag, int count){
    int foo=query(tag.getBytes(), count);
    if(foo==-1)
      return null;
    byte[] comment=user_comments[foo];
    for(int i=0; i<comment_lengths[foo]; i++){
      if(comment[i]=='='){
        return new String(comment, i+1, comment_lengths[foo]-(i+1));
      }
    }
    return null;
  }

  private int query(byte[] tag, int count){
    int i=0;
    int found=0;
    int fulltaglen=tag.length+1;
    byte[] fulltag=new byte[fulltaglen];
    System.arraycopy(tag, 0, fulltag, 0, tag.length);
    fulltag[tag.length]=(byte)'=';

    for(i=0; i<comments; i++){
      if(tagcompare(user_comments[i], fulltag, fulltaglen)){
        if(count==found){
          // We return a pointer to the data, not a copy
          //return user_comments[i] + taglen + 1;
          return i;
        }
        else{
          found++;
        }
      }
    }
    return -1;
  }

  int unpack(Buffer opb){
    int vendorlen=opb.read(32);
    if(vendorlen<0){
      clear();
      return (-1);
    }
    vendor=new byte[vendorlen+1];
    opb.read(vendor, vendorlen);
    comments=opb.read(32);
    if(comments<0){
      clear();
      return (-1);
    }
    user_comments=new byte[comments+1][];
    comment_lengths=new int[comments+1];

    for(int i=0; i<comments; i++){
      int len=opb.read(32);
      if(len<0){
        clear();
        return (-1);
      }
      comment_lengths[i]=len;
      user_comments[i]=new byte[len+1];
      opb.read(user_comments[i], len);
    }
    if(opb.read(1)!=1){
      clear();
      return (-1);

    }
    return (0);
  }

  int pack(Buffer opb){
    // preamble
    opb.write(0x03, 8);
    opb.write(_vorbis);

    // vendor
    opb.write(_vendor.length, 32);
    opb.write(_vendor);

    // comments
    opb.write(comments, 32);
    if(comments!=0){
      for(int i=0; i<comments; i++){
        if(user_comments[i]!=null){
          opb.write(comment_lengths[i], 32);
          opb.write(user_comments[i]);
        }
        else{
          opb.write(0, 32);
        }
      }
    }
    opb.write(1, 1);
    return (0);
  }

  public int header_out(Packet op){
    Buffer opb=new Buffer();
    opb.writeinit();

    if(pack(opb)!=0)
      return OV_EIMPL;

    op.packet_base=new byte[opb.bytes()];
    op.packet=0;
    op.bytes=opb.bytes();
    System.arraycopy(opb.buffer(), 0, op.packet_base, 0, op.bytes);
    op.b_o_s=0;
    op.e_o_s=0;
    op.granulepos=0;
    return 0;
  }

  void clear(){
    for(int i=0; i<comments; i++)
      user_comments[i]=null;
    user_comments=null;
    vendor=null;
  }

  public String getVendor(){
    return new String(vendor, 0, vendor.length-1);
  }

  public String getComment(int i){
    if(comments<=i)
      return null;
    return new String(user_comments[i], 0, user_comments[i].length-1);
  }

  public String toString(){
    String foo="Vendor: "+new String(vendor, 0, vendor.length-1);
    for(int i=0; i<comments; i++){
      foo=foo+"\nComment: "
          +new String(user_comments[i], 0, user_comments[i].length-1);
    }
    foo=foo+"\n";
    return foo;
  }
}
