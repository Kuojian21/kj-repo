#!/bin/bash

dir=$(cd $(dirname $0);pwd)
path=$1
cd ${path}
for gURL in $(cat ../${path}.list)
do
	OIFS=$IFS
	IFS="/"
	p=(${gURL})
	IFS=$OIFS

  pLen=${#p[@]}

	if [ ! -d ${p[pLen-2]} ]; then
		mkdir -p ${p[pLen-2]}
	fi

	cd ${p[pLen-2]}

	OIFS=$IFS
	IFS="."
	tp=(${p[pLen-1]})
	IFS=$OIFS

	if [ ! -d ${tp[0]} ]; then
		echo "=============================clone ${gURL}=============================="
		git clone "${gURL}"
		echo ${p[pLen-2]}/${tp[0]}
		cd ${tp[0]}
	else
		echo "===========================reset&pull ${gURL}==========================="
		cd ${tp[0]}
		git reset --hard
		git pull --rebase
	fi

	if [ -f "./pom.xml" ]; then
		echo "===========================mvn package ${gURL}=========================="
		mvn clean package -Dcheckstyle.skip=true -DskipTests -U #-T 4.0C
		mvn dependency:sources #-T 4.0C
		mvn dependency:resolve -Dclassifier=javadoc #-T 4.0C
	fi
	cd ${dir}/${path}
done

cd ${dir}
