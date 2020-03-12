#!/bin/bash

dir=$(cd $(dirname $0);pwd)
base_path=$1
base_url=$2
cd ${base_path}
for x in $(awk -F '/' '{print $(NF-1)"/"$NF}' ../${base_path}.list)
do
	OIFS=$IFS
	IFS="/"
	y=($x)
	IFS=$OIFS

	if [ ! -d ${y[0]} ]; then
		mkdir -p ${y[0]}
	fi

	cd ${y[0]}

	if [ ! -d ${y[1]} ]; then
		echo "=============================clone ${y[0]}/${y[1]}=============================="
		git clone "${base_url}:${y[0]}/${y[1]}.git"
		cd ${y[1]}
	else
		echo "===========================reset&pull ${y[0]}/${y[1]}==========================="
		cd ${y[1]}
		git reset --hard
		git pull --rebase
	fi

	if [ -f "./pom.xml" ]; then
		echo "===========================mvn package ${y[0]}/${y[1]}=========================="
		mvn clean package -Dcheckstyle.skip=true -DskipTests -U #-T 4.0C
		mvn dependency:sources #-T 4.0C
		mvn dependency:resolve -Dclassifier=javadoc #-T 4.0C
	fi
	cd ${dir}/${path}
done

cd ${dir}
