JAVA_HOME5=/u01/java/jre1.6.0_41
WORK_DIR=/home/webform/chat-trainning/subserver1
CLASSPATH=$WORK_DIR/run/subserver1.jar:$WORK_DIR/lib/apache-commons-pool-200905.jar:$WORK_DIR/lib/bsh-core-2.0b2.jar:$WORK_DIR/lib/commons-net-1.4.1.jar:$WORK_DIR/lib/FSSServerLib.jar:$WORK_DIR/lib/log4j-1.2.14.jar:$WORK_DIR/lib/ojdbc14_g.jar
APP_PATH=com.fss.thread.ChatManagerSubserver
pid_file=$WORK_DIR/services.pid
RUNNING=0
if [ -f $pid_file ]; then
	pid=`cat $pid_file`
	if [ "x$pid" != "x" ] && kill -0 $pid 2>/dev/null; then
		RUNNING=1
	fi
fi

if [ "$1" = "start" ] ; then
	cd $WORK_DIR
	if [ $RUNNING -eq 1 ]; then
		echo "service subserver1 has already started"
	else
		$JAVA_HOME5/bin/java -server -Xmx1024m -cp $CLASSPATH $APP_PATH &
		echo $! > $pid_file
		echo "service subserver1 started"
	fi	
elif [ "$1" = "stop" ] ; then
	if [ $RUNNING -eq 1 ]; then
		kill -9 $pid
		echo "service subserver1stopped"
	else
		echo "service subserver1 is not running"
	fi
else
	echo "Usage: $0 {  start | stop }"
fi
