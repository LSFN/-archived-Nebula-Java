"""
protoc.py: Protoc Builder for SCons

This Builder invokes protoc to generate C++ and Python
from a .proto file.

Original author: Scott Stafford

Modifed by Steven Haywood (steven.haywood.2010@my.bristol.ac.uk) for:
1) Java support
2) Single-project multiple protoc output langauges support via neat* interface

Note these changes break perfect backwards compatability. A simple change
of "env.Protoc(target.cpp, source.proto)" to "env.ProtocCPP(target, source)"
is all that is required. (similarly for Python). Note the environment variable
wrangling previously required to select output language is replaced with
explicit language builders and is therefore no longer required.

*: REMOVING the environment variable for the language you DO NOT want is,
in my (Steven Haywood's) opinion, NOT neat.

"""

__author__ = "Scott Stafford"

import SCons.Action
import SCons.Builder
import SCons.Defaults
import SCons.Node.FS
import SCons.Util

from SCons.Script import File, Dir

import os.path

protocs = 'protoc'

ProtocJavaAction = SCons.Action.Action(
                        '$PROTOCCOM_START $PROTOCJAVAFLAG $PROTOCCOM_END',
                        '$PROTOCCOM_START $PROTOCJAVAFLAG $PROTOCCOM_END')

ProtocPythonAction = SCons.Action.Action(
                        '$PROTOCCOM_START $PROTOCPYTHONFLAG $PROTOCCOM_END',
                        '$PROTOCCOM_START $PROTOCPYTHONFLAG $PROTOCCOM_END')

ProtocCPPAction = SCons.Action.Action(
                        '$PROTOCCOM_START $PROTOCCPPFLAG $PROTOCCOM_END',
                        '$PROTOCCOM_START $PROTOCCPPFLAG $PROTOCCOM_END')

def _ProtocEmitter(target, source, env, output_lang):
    """
    Generlised emitter function for protoc commands.

    output_lang must be one of 'java', 'python', 'cpp'
    """
    # @target can only be a directory (protoc limitation), therefore:
    # iff target is a list, containing one string,
    # which is a path to a directory, use that; else, use defaults.
    # @target will always be a list (SCons says so)
    if len(target) == 1 and os.path.isdir(str(target[0])):
        if output_lang == 'java':
            env['PROTOCJAVAOUTDIR'] = str(target[0])
        elif output_lang == 'cpp':
            env['PROTOCCPPOUTDIR'] = str(target[0])
        elif output_lang == 'python':
            env['PROTOCPYTHONOUTDIR'] = str(target[0])
        # else raise an exception or something.
    else:
        # in all other cases (ie: target isn't a directory),
        # set it to an empty list and the defaults will kick in
        target = []

    # Alter the path of the sources if required.
    # NB: This code is Scott's and I'm not entirely sure why it is needed.
    dirOfCallingSConscript = Dir('.').srcnode()

    env.Prepend(PROTOCPROTOPATH = dirOfCallingSConscript.path)

    source_with_corrected_path = []

    for src in source:
        commonprefix = os.path.commonprefix([dirOfCallingSConscript.path,
                                             src.srcnode().path])

        if len(commonprefix) > 0:
            source_with_corrected_path.append(
                            src.srcnode().path[len(commonprefix + os.sep):] )
        else:
            source_with_corrected_path.append( src.srcnode().path )

    source = source_with_corrected_path

    for src in source:
        # get the filename of the source file
        # (ie: foobar.proto from foo/bar/foobar.proto)
        modulename = os.path.basename(src)
        # Then take foobar from foobar.proto
        modulename = os.path.splitext(modulename)[0]

        if output_lang == 'cpp':
            base = os.path.join(env['PROTOCOUTDIR'], modulename)
            target.extend([base + '.pb.cc', base + '.pb.h'])
        elif output_lang == 'python':
            base = os.path.join(env['PROTOCPYTHONOUTDIR'], modulename)
            target.append(base + '_pb2.py')
        elif output_lang == 'java':
            # For reasons best known to the elder gods of google,
            # protoc capitalises the FIRST character of its java output files
            first_char = modulename[1]

            if not first_char.isupper():
                modulename = first_char.upper() + modulename[2:]

            base = os.path.join(env['PROTOCJAVAOUTDIR'], modulename)
            path = ''

            # For various really dumb reasons the target name we return MUST
            # be an actual target file when it comes to do compilation of other steps
            # However, in java mode protoc accepts an option to inflate the file down
            # a Java filepath (e.g. "com/example/subdomain/") instead of where we'd usually
            # expect. So we need to detect this and fix it.

            # First, crack open the source file and inspect it for the option
            srcfile = open(src)
            for line in srcfile:
                if "option" in line:
                    if "java_package" in line:
                        # This proto file sets the java pakcage name. We need to inflate
                        # this into a path and stick it on the front
                        words = line.strip().split(" ")
                        newpath = words[-1][1:-2].replace(".", "/") + "/"
                        path = os.path.join(env['PROTOCJAVAOUTDIR'], newpath)
                    elif "java_outer_classname" in line:
                        # This proto file has overrriden what it's output filename will be
                        # We assume that it will be the last 'word' in the line, and be
                        # surrounded by " and have a semicolon at the end. As per spec
                        words = line.strip().split(" ")
                        base = words[-1][1:-2]

            # Now after all of that faffing about and special cases we can actually set this 
            # source file's target
            target.append(os.path.join(path, base) + '.java')

    try:
        target.append(env['PROTOCFDSOUT'])
    except KeyError:
        pass

    return target, source

def ProtocJavaEmitter(target, source, env):
    # Use generalised emitter:
    return _ProtocEmitter(target, source, env, 'java')

def ProtocPythonEmitter(target, source, env):
    return _ProtocEmitter(target, source, env, 'python')

def ProtocCPPEmitter(target, source, env):
    return _ProtocEmitter(target, source, env, 'cpp')

# Define the three language builders:
ProtocJavaBuilder = SCons.Builder.Builder(action = ProtocJavaAction,
                                   emitter = ProtocJavaEmitter,
                                   srcsuffix = '$PROTOCSRCSUFFIX')

ProtocPythonBuilder = SCons.Builder.Builder(action = ProtocPythonAction,
                                   emitter = ProtocPythonEmitter,
                                   srcsuffix = '$PROTOCSRCSUFFIX')

ProtocCPPBuilder = SCons.Builder.Builder(action = ProtocCPPAction,
                                   emitter = ProtocCPPEmitter,
                                   srcsuffix = '$PROTOCSRCSUFFIX')

_builder_dict = {'ProtocCPP': ProtocCPPBuilder,
                 'ProtocPython': ProtocPythonBuilder,
                 'ProtocJava': ProtocJavaBuilder}

def generate(env):
    """
    Add Builders and construction variables for protoc to an Environment.
    """

    for key in _builder_dict.keys():
        try:
            bld = env['BUILDERS'][key]
            #~ print "Found {0} in env['BUILDERS'] already!".format(str(key))
        except KeyError:
            bld = _builder_dict[key]
            env['BUILDERS'][key] = bld
            #~ print "Added {0} to env['BUILDERS']".format(str(key))

    env['PROTOC']        = env.Detect(protocs) or 'protoc'
    env['PROTOCFLAGS']   = SCons.Util.CLVar('')
    env['PROTOCPROTOPATH'] = SCons.Util.CLVar('')

    # Need to insert an option into the middle of the command,
    # so use start and end COM strings
    env['PROTOCCOM_START']     = """$PROTOC ${["-I%s"%x for x in PROTOCPROTOPATH]} $PROTOCFLAGS ${PROTOCFDSOUT and ("-o"+PROTOCFDSOUT) or ""}"""

    env['PROTOCCOM_END'] = '${SOURCES}'

    # Provide environment variables for each possible language output
    # so that users can easily put java files in a different directory
    # to python files etc. Leave the python and CPP defaults as they were
    # previously. Java outputs should probably go in a 'java' directory.
    env['PROTOCOUTDIR'] = '${SOURCE.dir}'
    env['PROTOCPYTHONOUTDIR'] = 'python'
    env['PROTOCJAVAOUTDIR'] = 'java'

    # The variable protoc option, provided so that if users wish to alter
    # flags for a specific output language for their project, they can.
    # Obviously, the base --<lang>_out=<dir> part needs to remain.
    env['PROTOCJAVAFLAG'] = '--java_out=${PROTOCJAVAOUTDIR}'
    env['PROTOCPYTHONFLAG'] = '--python_out=${PROTOCPYTHONOUTDIR}'
    env['PROTOCCPPFLAG'] = '--cpp_out=${PROTOCOUTDIR}'

    env['PROTOCSRCSUFFIX']  = '.proto'

def exists(env):
    return env.Detect(protocs)
