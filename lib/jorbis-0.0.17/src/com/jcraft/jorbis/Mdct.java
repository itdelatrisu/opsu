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

class Mdct{

  int n;
  int log2n;

  float[] trig;
  int[] bitrev;

  float scale;

  void init(int n){
    bitrev=new int[n/4];
    trig=new float[n+n/4];

    log2n=(int)Math.rint(Math.log(n)/Math.log(2));
    this.n=n;

    int AE=0;
    int AO=1;
    int BE=AE+n/2;
    int BO=BE+1;
    int CE=BE+n/2;
    int CO=CE+1;
    // trig lookups...
    for(int i=0; i<n/4; i++){
      trig[AE+i*2]=(float)Math.cos((Math.PI/n)*(4*i));
      trig[AO+i*2]=(float)-Math.sin((Math.PI/n)*(4*i));
      trig[BE+i*2]=(float)Math.cos((Math.PI/(2*n))*(2*i+1));
      trig[BO+i*2]=(float)Math.sin((Math.PI/(2*n))*(2*i+1));
    }
    for(int i=0; i<n/8; i++){
      trig[CE+i*2]=(float)Math.cos((Math.PI/n)*(4*i+2));
      trig[CO+i*2]=(float)-Math.sin((Math.PI/n)*(4*i+2));
    }

    {
      int mask=(1<<(log2n-1))-1;
      int msb=1<<(log2n-2);
      for(int i=0; i<n/8; i++){
        int acc=0;
        for(int j=0; msb>>>j!=0; j++)
          if(((msb>>>j)&i)!=0)
            acc|=1<<j;
        bitrev[i*2]=((~acc)&mask);
        //	bitrev[i*2]=((~acc)&mask)-1;
        bitrev[i*2+1]=acc;
      }
    }
    scale=4.f/n;
  }

  void clear(){
  }

  void forward(float[] in, float[] out){
  }

  float[] _x=new float[1024];
  float[] _w=new float[1024];

  synchronized void backward(float[] in, float[] out){
    if(_x.length<n/2){
      _x=new float[n/2];
    }
    if(_w.length<n/2){
      _w=new float[n/2];
    }
    float[] x=_x;
    float[] w=_w;
    int n2=n>>>1;
    int n4=n>>>2;
    int n8=n>>>3;

    // rotate + step 1
    {
      int inO=1;
      int xO=0;
      int A=n2;

      int i;
      for(i=0; i<n8; i++){
        A-=2;
        x[xO++]=-in[inO+2]*trig[A+1]-in[inO]*trig[A];
        x[xO++]=in[inO]*trig[A+1]-in[inO+2]*trig[A];
        inO+=4;
      }

      inO=n2-4;

      for(i=0; i<n8; i++){
        A-=2;
        x[xO++]=in[inO]*trig[A+1]+in[inO+2]*trig[A];
        x[xO++]=in[inO]*trig[A]-in[inO+2]*trig[A+1];
        inO-=4;
      }
    }

    float[] xxx=mdct_kernel(x, w, n, n2, n4, n8);
    int xx=0;

    // step 8

    {
      int B=n2;
      int o1=n4, o2=o1-1;
      int o3=n4+n2, o4=o3-1;

      for(int i=0; i<n4; i++){
        float temp1=(xxx[xx]*trig[B+1]-xxx[xx+1]*trig[B]);
        float temp2=-(xxx[xx]*trig[B]+xxx[xx+1]*trig[B+1]);

        out[o1]=-temp1;
        out[o2]=temp1;
        out[o3]=temp2;
        out[o4]=temp2;

        o1++;
        o2--;
        o3++;
        o4--;
        xx+=2;
        B+=2;
      }
    }
  }

  private float[] mdct_kernel(float[] x, float[] w, int n, int n2, int n4,
      int n8){
    // step 2

    int xA=n4;
    int xB=0;
    int w2=n4;
    int A=n2;

    for(int i=0; i<n4;){
      float x0=x[xA]-x[xB];
      float x1;
      w[w2+i]=x[xA++]+x[xB++];

      x1=x[xA]-x[xB];
      A-=4;

      w[i++]=x0*trig[A]+x1*trig[A+1];
      w[i]=x1*trig[A]-x0*trig[A+1];

      w[w2+i]=x[xA++]+x[xB++];
      i++;
    }

    // step 3

    {
      for(int i=0; i<log2n-3; i++){
        int k0=n>>>(i+2);
        int k1=1<<(i+3);
        int wbase=n2-2;

        A=0;
        float[] temp;

        for(int r=0; r<(k0>>>2); r++){
          int w1=wbase;
          w2=w1-(k0>>1);
          float AEv=trig[A], wA;
          float AOv=trig[A+1], wB;
          wbase-=2;

          k0++;
          for(int s=0; s<(2<<i); s++){
            wB=w[w1]-w[w2];
            x[w1]=w[w1]+w[w2];

            wA=w[++w1]-w[++w2];
            x[w1]=w[w1]+w[w2];

            x[w2]=wA*AEv-wB*AOv;
            x[w2-1]=wB*AEv+wA*AOv;

            w1-=k0;
            w2-=k0;
          }
          k0--;
          A+=k1;
        }

        temp=w;
        w=x;
        x=temp;
      }
    }

    // step 4, 5, 6, 7
    {
      int C=n;
      int bit=0;
      int x1=0;
      int x2=n2-1;

      for(int i=0; i<n8; i++){
        int t1=bitrev[bit++];
        int t2=bitrev[bit++];

        float wA=w[t1]-w[t2+1];
        float wB=w[t1-1]+w[t2];
        float wC=w[t1]+w[t2+1];
        float wD=w[t1-1]-w[t2];

        float wACE=wA*trig[C];
        float wBCE=wB*trig[C++];
        float wACO=wA*trig[C];
        float wBCO=wB*trig[C++];

        x[x1++]=(wC+wACO+wBCE)*.5f;
        x[x2--]=(-wD+wBCO-wACE)*.5f;
        x[x1++]=(wD+wBCO-wACE)*.5f;
        x[x2--]=(wC-wACO-wBCE)*.5f;
      }
    }
    return (x);
  }
}
