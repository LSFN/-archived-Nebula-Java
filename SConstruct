"""
Main build file for LSFN's Nebula server

Whilst you can alter various important things,
such as where the source files live and where the
class files go, it is recommended you don't.
"""

SOURCE_DIRECTORY = 'src/'
BUILD_DIRECTORY = 'build/'
OUTPUT_JAR_FILENAME = 'nebula.jar'

# Scons has a concept of a build 'environment', so this is needed:
env = Environment()

# task to make the build directory exist (if it doesn't already):
make_build_directory = env.Command(BUILD_DIRECTORY, 
								   None, 
								   Mkdir(BUILD_DIRECTORY))

# task for actually doing the java build:
java_build = env.Java(target = BUILD_DIRECTORY, source = SOURCE_DIRECTORY)

# task for producing console-pc.jar:
jar_build = env.Jar(target = OUTPUT_JAR_FILENAME, source = SOURCE_DIRECTORY)

# Tell scons that one must build the java files before JARing them:
env.Depends(jar_build, java_build)

# Tell scons that the default action is to do the jar build:
env.Default(jar_build)
