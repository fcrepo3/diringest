Samples
=======

Each zip file in this directory is a sample SIP that can
be converted to a set of FOXML files using the sip2fox 
utility (or the diringest service).

one.zip
-------
Description : A SIP with no binary data and one dmdSec,
              referenced from one div.
Contents    : METS.xml
Output      : One FOXML file with one inline XML datastream.

two.zip
-------
Description : A SIP with one dmdSec and two files, one internal
              xml and the other a reference to a file in the SIP.
Contents    : METS.xml
              content/readme.txt 
Output      : Two FOXML files, the first containing one inline datastream
              and the other containing two datastreams: one inline xml
              and the other, managed.

three.zip
---------
Description : A SIP with several miscellaneous files of various
              types.  This SIP should be used in conjunction with
              crules-sample-three.xml
Contents    : METS.xml
              My Documents/Music/08_ThieveryCorporation_DC3000.mp3
              My Documents/Music/Mine/MyAlbumCover.jpg
              My Documents/Music/Mine/MyAlbumLyrics.txt    
              My Documents/School/Schedule.xls
              My Documents/Papers/Fahrenheit451.doc          
Output      : A FOXML file for each file and directory.
              ... with a dublin core datastream for each.
              ... and a license datastream for each file.