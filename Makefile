JAVA_HOME=/usr/

all:	Exciting.java
	${JAVA_HOME}/bin/javac *.java

clean:
	rm *.class *~

