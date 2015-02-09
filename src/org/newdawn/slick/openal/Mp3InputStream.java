/*
Copyright (c) 2013, Slick2D

All rights reserved.

Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

* Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
* Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
* Neither the name of the Slick2D nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS gAS ISh AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package org.newdawn.slick.openal;

import java.io.IOException;
import java.io.InputStream;

import sun.font.EAttribute;
import javazoom.jl.decoder.*;

public class Mp3InputStream extends InputStream implements AudioInputStream {

	Bitstream bitstream;
	Decoder decoder;
	Header header;
	SampleBuffer buf;
	
	int channels;
	int sampleRate;
	
	int bufLen = 0;
	boolean atEnd=false;
	int bpos;	//byte pos
	
	public Mp3InputStream(InputStream resourceAsStream) {
		decoder = new Decoder();
		bitstream = new Bitstream(resourceAsStream);
		try {
			header = bitstream.readFrame();
		} catch (BitstreamException e) {
			e.printStackTrace();
		}
		
		channels = header.mode()==Header.SINGLE_CHANNEL?1:2;
		sampleRate = header.frequency();
		
		buf = new SampleBuffer(sampleRate, channels);
		decoder.setOutputBuffer(buf);
		//*
		try {
			decoder.decodeFrame(header, bitstream);
		} catch (DecoderException e) {
			e.printStackTrace();
		}
		//*/
		bufLen = buf.getBufferLength();
		bitstream.closeFrame();
	}
	@Override
	public int read() throws IOException {
		if(atEnd())
			return -1;
		while(bpos/2>=bufLen){
			try {
				header = bitstream.readFrame();
				if(header == null){
					buf.clear_buffer();
					
					atEnd = true;
					return -1;
				}
				buf.clear_buffer();
				decoder.decodeFrame(header, bitstream);
				bufLen = buf.getBufferLength();
				bitstream.closeFrame();
			} catch (DecoderException e) {
				e.printStackTrace();
			} catch (BitstreamException e) {
				e.printStackTrace();
			}
			bpos=0;
		}
		int npos = bpos/2;
		bpos++;
		
		if(bpos%2==0)
			return (buf.getBuffer()[npos]>>8)&0xff;
		else
			return (buf.getBuffer()[npos])&0xff;
	}
	@Override
	public boolean atEnd() {
		return atEnd;
	}
	@Override
	public int getChannels() {
		return channels;
	}
	@Override
	public int getRate() {
		return sampleRate;
	}
	/**
	 * @see java.io.InputStream#read(byte[], int, int)
	 */
	public int read(byte[] b, int off, int len) throws IOException {
		for (int i=0;i<len;i++) {
			try {
				int value = read();
				if (value >= 0) {
					b[i] = (byte) value;
				} else {
					if (i == 0) {						
						return -1;
					} else {
						return i;
					}
				}
			} catch (IOException e) {
				//Log.error(e);
				e.printStackTrace();
				return i;
			}
		}
		
		return len;
	}

	/**
	 * @see java.io.InputStream#read(byte[])
	 */
	public int read(byte[] b) throws IOException {
		return read(b, 0, b.length);
	}
	@Override
	public long skip(long length) {
		//System.out.println("skip"+length);
		int skiped = 0;
		if(bufLen<=0){
			System.out.println("We don't know buf Length yet");
			//throw new Error("We don't know buf Length yet");
		}
		while(skiped+bufLen*2<length){
			try {
				header = bitstream.readFrame();
				if(header == null){
					//System.out.println("Header is null");
					atEnd = true;
					return -1;
				}
				//last frame that won't be skiped so better read it
				if(skiped+bufLen*2*4 >=length || bufLen<=0){
					buf.clear_buffer();
					decoder.decodeFrame(header, bitstream);
					bufLen = buf.getBufferLength();
				}
				skiped+=bufLen*2-bpos;
					
				bitstream.closeFrame();
				bpos=0;
			} catch (BitstreamException e) {
				e.printStackTrace();
			}catch (DecoderException e) {
				e.printStackTrace();
			}
		}
		if(bufLen*2-bpos>length-skiped){
			bpos+=length-skiped;
			skiped+=length-skiped;
		}
		
		return skiped;
		
	}
}
