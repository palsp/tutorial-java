compile:
	javac com/learnsecurity/SimpleWebServer.java

app:
	@$(MAKE) compile
	java com.learnsecurity.SimpleWebServer