#!/bin/bash
# Script for starting up Binder#
#jboss_dir=/usr/local/jboss-4.0.3SP1/
#jboss_cp=

#jboss_cp=${jboss_cp}${jboss_dir}server/all/deploy/jboss-annotations-ejb3.jar:
#jboss_cp=${jboss_cp}${jboss_dir}server/all/deploy/jboss-aop-jdk50.deployer/jboss-aop-jdk50.jar:
#jboss_cp=${jboss_cp}${jboss_dir}server/all/deploy/jboss-aop-jdk50.deployer/jboss-aspect-library-jdk50.jar:
#jboss_cp=${jboss_cp}${jboss_dir}client/jnp-client.jar:
#jboss_cp=${jboss_cp}${jboss_dir}client/jbossall-client.jar:
#jboss_cp=${jboss_cp}${jboss_dir}client/jboss-common-client.jar:
#jboss_cp=${jboss_cp}${jboss_dir}server/all/deploy/jboss-ejb3.jar:
#jboss_cp=${jboss_cp}${jboss_dir}server/all/deploy/jboss-ejb3x.jar:
#jboss_cp=${jboss_cp}${jboss_dir}server/all/lib/jboss-j2ee.jar:
#jboss_cp=${jboss_cp}${jboss_dir}server/all/lib/jboss-remoting.jar:
#jboss_cp=${jboss_cp}${jboss_dir}server/all/lib/jboss-serialization.jar:
#jboss_cp=${jboss_cp}${jboss_dir}server/all/lib/jboss-transaction.jar:
#jboss_cp=${jboss_cp}${jboss_dir}server/all/lib/jboss.jar:
#jboss_cp=${jboss_cp}${jboss_dir}server/all/deploy/hibernate-entitymanager.jar:
#jboss_cp=${jboss_cp}${jboss_dir}server/all/deploy/hibernate-entitymanager.jar:
#jboss_cp=${jboss_cp}${jboss_dir}server/all/deploy/hibernate3.jar:
#jboss_cp=${jboss_cp}${jboss_dir}server/all/deploy/hibernate-annotations.jar

other_cp=gel-remote.jar
log4j_cp=log4j-1.2.15.jar
javaee_cp=javaee.jar
binder_cp=Binder.jar
pbfs_cp=pbfs.jar

echo Starting Binder...
#java -cp "${jboss_cp}:${other_cp}:${log4j_cp}:${javaee_cp}:bin/" yu.ac.bg.rcub.binder.Binder "binder.properties"
#java -cp "${jboss_cp}:${other_cp}:${log4j_cp}:${javaee_cp}:${binder_cp}:${pbfs_cp}" yu.ac.bg.rcub.binder.Binder "binder.properties"

java -cp $(echo lib/*.jar | sed 's/ /:/g'):bin/ yu.ac.bg.rcub.binder.Binder "binder.properties"

echo Binder Stopped.

