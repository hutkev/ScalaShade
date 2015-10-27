ScalaShade
==========

A class/jar re-writer that can be used to correct the @ScalaSignature annotation found in compiled Scala classes after a shading operation (the renaming of namespaces) has been performed on the classes.

The Scala compiler places a 'ScalaSignature' annotation in some compiled classes to make additional runtime type information available to the Scala libraries that can't be normally encoded in a class file. The annotation contains a string which when decoded yields the type information. This type information can contain namespaces that need updating to reflect the shading operations to avoid breaking the Scala code.

If you suspect you might have this problem then you can confirm by running 'scalap' (part of the normal Scala distribution) and inspecting the output. If there are still references to the pre-shading namespaces then they will need correcting. You can of course also use 'scalap' to confirm that ScalaShade has done its job.

Example
=======

To correct a jar use:

	java -jar scalashade.jar <input.jar> <output.jar> <replace-namespace> <with-namespace>

To correct org.apache references to shaded.org.apache use 
 
	java -jar scalashade.jar -v target/myjar.jar target/corrected.jar org.apache shaded.org.apache 
 
-v turns on logging of classes modified


Alternatively replace jars by class files to operate on single files

	java -jar scalashade.jar -v target/myclass.class target/corrected.class org.apache shaded.org.apache


Namespaces
==========

The replacement only operates on absolute namespaces within the type information, so "org.apache" will change any use of a namespace starting with "org.apache" but would not alter "foo.org.apache".
 
Building
========

To build using maven run

	mvn package

Tool Integration
================

I have deliberately tried to keep very single purpose so that's it easy to integrate into other shading tools which is where it or similar needs to belong. If you need any help doing that please feel free to give me a shout.   

 
