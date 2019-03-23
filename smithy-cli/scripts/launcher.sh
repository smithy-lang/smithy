#!/bin/sh
DIR=\${0%/*}
cd \$DIR

if [ "\$#" -eq 1 ] && [ "\$1" == "optimize" ]; then
  echo "Performing optimizations to make the Smithy CLI startup faster..."
  echo '{"smithy":"1.0"}' > _temp.smithy.json
  ./java -XX:DumpLoadedClassList=classes.lst -m ${moduleName}/${mainClassName} validate _temp.smithy.json > /dev/null
  rm _temp.smithy.json
  ./java -Xshare:dump -XX:SharedClassListFile=classes.lst -XX:SharedArchiveFile=app-cds.jsa > /dev/null
  rm classes.lst
  echo "Optimizations complete"
  exit 0
fi

./java ${jvmArgs} -m ${moduleName}/${mainClassName} ${args} \$@
