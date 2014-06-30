
                                    JOrbis

                     a pure Java(TM) Ogg Vorbis decoder
                               by ymnk, JCraft,Inc.

                        http://www.jcraft.com/jorbis/

Last modified: Mon May  7 14:31:03 UTC 2001

Description
===========

JOrbis is a pure Java Ogg Vorbis decoder.
JOrbis accepts Ogg Vorbis bitstreams and decodes them to raw PCMs.


Documentation
=============

Read before asking.

* README files all over the source tree have info related to the stuff
in the directories. 


Directories & Files in the Source Tree
======================================

* com/ has source trees of JOrbis.
* player/ has source and binaries of pure Java Ogg Vorbis player.


Copyrights & Disclaimers
========================

JOrbis is copyrighted by JCraft Inc. and is licensed through the
GNU Lesser General Public License. 
Read the COPYING.LIB file for the complete license.


Credits
=======
All credits goes to authors, Moty<monty@xiph.org>,The XIPHOPHORUS Company 
and contributers of Ogg Vorbis.


What is Ogg Vorbis?
===================
Here is a quote from README of Ogg Vorbis CODEC from 
the Developer site for Ogg Vorbis(http://xiph.org/ogg/vorbis/).

  "Vorbis is a general purpose audio and music encoding format
   contemporary to MPEG-4's AAC and TwinVQ, the next generation beyond
   MPEG audio layer 3. Unlike the MPEG sponsored formats (and other
   proprietary formats such as RealAudio G2 and Windows' flavor of the
   month), the Vorbis CODEC specification belongs to the public domain.
   All the technical details are published and documented, and any
   software entity may make full use of the format without royalty or
   patent concerns."


Why JOrbis?
===========
We sympathize the aim of Ogg project. 
JOrbis is our contribution to the Ogg project in our style.
We think the ubiquity of Vorbis decoder will leverage the
popularity of Ogg Vorbis. We hope JOrbis will run on any platform,
any devices and any web browsers, which support Java and
every people will enjoy streamed musics without without royalty or
patent concerns.


Features
========
  * JOrbis is in pure JavaTM.
    JOrbis will run on JDK1.0.* or higher and may run on J2METM.
  * JOrbis is under LGPL.


Current Status
==============
The sound quality of outputs from JOrbis has been improved.
If you had tried last version(0.0.1) and been disappointed with its quality,
try current version. It will be worth trying.
However, in current implementation, the efficiency has not been cared and
many CPU resources are required. Much work must be done to solve this problem.


How To Try JOrbis
=================
A sample program DecodeExample.java has been included in an archive.<br>
If you are on RedHat's GNU/Linux box, just try,

  $ cat foo.ogg | java com.jcraft.jorbis.DecodeExample | esdcat

If outputs are noisy, save outputs in a file then cat it to esdcat.
Ogg files are available at http://vorbis.com/listen.html
and if you have Ogg Vorbis CODEC from xiph.org, you can make ogg files 
by your self.

How To Play JOrbisPlayer
========================
The Ogg Voribs player, 'JOrbisPlayer', is available at the 'player' directory.
It is a pure Java program, but it uses Java Sound APIs. 
If you have J2SE V.1.3 Java VM, you can enjoy it.
 *step1: Copy contents in the 'player' directory to some directory,
         which is accessible via the http server. 
 *step2: Copy some ogg files to that directory. 
 *step3: Change directory to that directory, make 'playlist' file.
         For example, 'ls *.ogg > playlist'
 *step4: Open 'JOrbisPlayer.html' file by a web browser via http server.
         JOrbisPlayer will run on a web browser automatically.  
'JOrbisPlayer' also works as an application, so just try 'java JOrbisPlayer' 
in the 'player' directory. For example, on GNU/Linux,

 $ cd player
 $ export CLASSPATH=.:..
 $ java JOrbisPlayer foo.ogg bar.ogg http://shomewahre/goo.ogg

If you don't have the direct connections to the Internet and
you have the http proxy server at 192.168.0.1:80, try as follows,
 $ java -Dhttp.proxyHost=192.168.0.1 -Dhttp.proxyPort=80 JOrbisPlayer http://shomewahre/goo.ogg

In our experiences, if your machine has a Cerelon, you will be able to 
enjoy JOrbisPlayer.


If you have any comments, suggestions and questions, write us 
at jorbis@jcraft.com
