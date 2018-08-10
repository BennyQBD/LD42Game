find -wholename "./lib/*.jar" -printf ".%p:" > classpath2.txt
find -wholename "./lib/*.jar" -printf "%p:" > classpath.txt
find -wholename "./lib/*.jar" -printf " %p \n" > classpath3.txt
