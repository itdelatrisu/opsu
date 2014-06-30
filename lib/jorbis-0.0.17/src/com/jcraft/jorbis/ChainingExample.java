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

class ChainingExample{
  public static void main(String[] arg){
    VorbisFile ov=null;

    try{
      if(arg.length>0){
        ov=new VorbisFile(arg[0]);
      }
      else{
        ov=new VorbisFile(System.in, null, -1);
      }
    }
    catch(Exception e){
      System.err.println(e);
      return;
    }

    if(ov.seekable()){
      System.out.println("Input bitstream contained "+ov.streams()
          +" logical bitstream section(s).");
      System.out.println("Total bitstream playing time: "+ov.time_total(-1)
          +" seconds\n");
    }
    else{
      System.out.println("Standard input was not seekable.");
      System.out.println("First logical bitstream information:\n");
    }

    for(int i=0; i<ov.streams(); i++){
      Info vi=ov.getInfo(i);
      System.out.println("\tlogical bitstream section "+(i+1)+" information:");
      System.out.println("\t\t"+vi.rate+"Hz "+vi.channels+" channels bitrate "
          +(ov.bitrate(i)/1000)+"kbps serial number="+ov.serialnumber(i));
      System.out.print("\t\tcompressed length: "+ov.raw_total(i)+" bytes ");
      System.out.println(" play time: "+ov.time_total(i)+"s");
      Comment vc=ov.getComment(i);
      System.out.println(vc);
    }
    //ov.clear();
  }
}
