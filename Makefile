all: compile
	@echo -e '[INFO] Done!'
clean:
	@echo -e '[INFO] Cleaning Up..'	
	@-rm -rf bin/cs455/**/**/*.class

compile: 
	@echo -e '[INFO] Compiling the Source..'
	@mkdir -p bin
	@javac bin src/cs455/**/**/*.java
	
	
