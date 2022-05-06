#!/usr/bin/env bash

SCRIPT_DIR=$(cd `dirname $0` && pwd)
echo "SCRIPT_DIR: $SCRIPT_DIR"

############ workaround ########################
#!/bin/bash


####!/usr/bin/env bash

PWD=$(cd `dirname ${BASH_SOURCE[0]}` && pwd)

export MALLOC_ARENA_MAX=1 # Iceberg's requirement
export TZ='America/Los_Angeles' # some D4J's requires this specific TimeZone

export _JAVA_OPTIONS="-Xmx6144M -XX:MaxHeapSize=2048M"
export MAVEN_OPTS="-Xmx1024M"
export ANT_OPTS="-Xmx6144M -XX:MaxHeapSize=2048M"

export LC_ALL=en_US.UTF-8
export LANG=en_US.UTF-8
export LANGUAGE=en_US.UTF-8

# Speed up grep command
alias grep="LANG=C grep"

#
# Prints error message to the stdout and exit.
#
die() {
  echo "$@" >&2
  exit 1
}

#
# Collect the list of unit test methods.
#
_collect_list_of_unit_tests() {
  local USAGE="Usage: ${FUNCNAME[0]} <checkout_dir> <output_file> <test_classpath> <test_classes_dir> <all_tests_file>"
  if [ "$#" != 5 ]; then
    echo "$USAGE" >&2
    return 1
  fi

  [ "$GZOLTAR_CLI_JAR" != "" ] || die "[ERROR] GZOLTAR_CLI_JAR is not set!"
  [ -s "$GZOLTAR_CLI_JAR" ] || die "[ERROR] $GZOLTAR_CLI_JAR does not exist!"

  local checkout_dir="$1"
  [ -d "$checkout_dir" ] || die "[ERROR] $checkout_dir does not exist!"
  local output_file="$2"
  >"$output_file" || die "[ERROR] Cannot write to $output_file!"

  test_classpath="$3"
  test_classes_dir="$4"

  local all_tests_file="$5"  #"/mnt/benchmarks/buggylocs/Defects4J/Defects4J_${pid}_${bid}/testClasses.txt"
  #local relevant_tests_file="$D4J_HOME_FOR_FL/framework/projects/$pid/relevant_tests/$bid"
  [ -s "$all_tests_file" ] || die "[ERROR] $all_tests_file does not exist or it is empty!"
  echo "[DEBUG] all_tests_file: $all_tests_file" >&2

<<C1
  java -cp $D4J_HOME_FOR_FL/framework/projects/lib/junit-4.11.jar:$test_classpath:$GZOLTAR_CLI_JAR \
    com.gzoltar.cli.Main listTestMethods \
      "$test_classes_dir" \
      --outputFile "$output_file" \
      --includes $(cat "$all_tests_file" | sed 's/$/#*/' | sed ':a;N;$!ba;s/\n/:/g') || die "GZoltar listTestMethods command has failed!"
  [ -s "$output_file" ] || die "[ERROR] $output_file does not exist or it is empty!"
C1

  java -cp $JUNIT_JAR:$test_classpath:$GZOLTAR_CLI_JAR \
    com.gzoltar.cli.Main listTestMethods \
      "$test_classes_dir" \
      --outputFile "$output_file" --includes $(cat "$all_tests_file" | sed 's/$/#*/' | sed ':a;N;$!ba;s/\n/:/g') || die "GZoltar listTestMethods command has failed!"
  [ -s "$output_file" ] || die "[ERROR] $output_file does not exist or it is empty!"
  return 0
}

#
# Collect the list of classes (and inner classes) that might be faulty.
#
_collect_list_of_likely_faulty_classes() {
  local USAGE="Usage: ${FUNCNAME[0]} <checkout_dir> <output_file> <src_classes_file>"
  if [ "$#" != 3 ]; then
    echo "$USAGE" >&2
    return 1
  fi

  local checkout_dir="$1"
  [ -d "$checkout_dir" ] || die "[ERROR] $checkout_dir does not exist!"
  local output_file="$2"
  >"$output_file" || die "[ERROR] Cannot write to $output_file!"

  # /mnt/benchmarks/buggylocs/Defects4J/Defects4J_Closure_103/srcClasses.txt
  local src_classes_file="$3"    #"/mnt/benchmarks/buggylocs/Defects4J/Defects4J_${pid}_${bid}/srcClasses.txt"
  #local loaded_classes_file="$D4J_HOME_FOR_FL/framework/projects/$pid/loaded_classes/$bid.src"
  [ -s "$src_classes_file" ] || die "[ERROR] $src_classes_file does not exist or it is empty!"
  echo "[DEBUG] src_classes_file: $src_classes_file" >&2

  # "normal" classes
  local normal_classes=$(cat "$src_classes_file" | sed 's/$/:/' | sed ':a;N;$!ba;s/\n//g')
  [ "$normal_classes" != "" ] || die "[ERROR] List of classes is empty!"
  local inner_classes=$(cat "$src_classes_file" | sed 's/$/$*:/' | sed ':a;N;$!ba;s/\n//g')
  [ "$inner_classes" != "" ] || die "[ERROR] List of inner classes is empty!"

  echo "$normal_classes$inner_classes" > "$output_file"
  return 0
}

#
# Runs GZoltar fault localization tool on a specific D4J's project-bug.
#
_run_gzoltar() {
  local USAGE="Usage: ${FUNCNAME[0]} <bug_dir> <data_dir> <test_classpath> <test_classes_dir> <src_classes_file> <src_classes_dir> <all_tests_file>"
  if [ "$#" != 7 ]; then
    echo "$USAGE" >&2
    return 1
  fi

  [ "$GZOLTAR_CLI_JAR" != "" ] || die "[ERROR] GZOLTAR_CLI_JAR is not set!"
  [ -s "$GZOLTAR_CLI_JAR" ] || die "[ERROR] $GZOLTAR_CLI_JAR does not exist or it is empty!"

  [ "$GZOLTAR_AGENT_JAR" != "" ] || die "[ERROR] GZOLTAR_AGENT_JAR is not set!"
  [ -s "$GZOLTAR_AGENT_JAR" ] || die "[ERROR] $GZOLTAR_AGENT_JAR does not exist or it is empty!"

  local bug_dir="$1"
  local data_dir="$2"
  local test_classpath="$3"
  local test_classes_dir="$4"
  local src_classes_file="$5"
  local src_classes_dir="$6"
  local all_tests_file="$7"

  local unit_tests_file="$data_dir/unit_tests.txt"
  >"$unit_tests_file" || die "[ERROR] Cannot write to $unit_tests_file!"
  _collect_list_of_unit_tests "$bug_dir" "$unit_tests_file" "$test_classpath" "$test_classes_dir" "$all_tests_file"

  if [ $? -ne 0 ]; then
    echo "[ERROR] Collection of unit test cases of the program has failed!" >&2
    return 1
  fi

  local classes_to_debug_file="$data_dir/classes_to_debug.txt"
  >"$classes_to_debug_file" || die "[ERROR] Cannot write to $classes_to_debug_file!"
  _collect_list_of_likely_faulty_classes "$bug_dir" "$classes_to_debug_file" "$src_classes_file"
  if [ $? -ne 0 ]; then
    echo "[ERROR] Collection of likely faulty classes of the program has failed!" >&2
    return 1
  fi
  local classes_to_debug=$(cat "$classes_to_debug_file")

  # Some export commands might have removed some build files
#  (cd "$bug_dir" > /dev/null 2>&1 && \
#     $D4J_HOME_FOR_FL/framework/bin/defects4j compile > /dev/null 2>&1) || die "[ERROR] Failed to compile the project!"

  local ser_file="$data_dir/gzoltar.ser"
#<<C1
  echo "[INFO] Start: $(date)" >&2
  (cd "$bug_dir" > /dev/null 2>&1 && \
    java -XX:MaxPermSize=2048M -javaagent:$GZOLTAR_AGENT_JAR=destfile=$ser_file,buildlocation=$src_classes_dir,includes=$classes_to_debug,excludes="",inclnolocationclasses=false,output="FILE" \
      -cp $src_classes_dir:$JUNIT_JAR:$test_classpath:$GZOLTAR_CLI_JAR \
      com.gzoltar.cli.Main runTestMethods \
        --testMethods "$unit_tests_file" \
        --collectCoverage)
#C1
  

  if [ $? -ne 0 ]; then
    echo "[ERROR] GZoltar runTestMethods command has failed for program!" >&2
    return 1
  fi
  [ -s "$ser_file" ] || die "[ERROR] $ser_file does not exist or it is empty!"

  echo "[INFO] End: $(date)" >&2

  return 0
}

#
# Generates a text-based fault localization report given a previously computed
# .ser file.
#
_generate_fault_localization_report() {
  local USAGE="Usage: ${FUNCNAME[0]} <ser_file_path> <output_dir> <test_classpath> <src_classes_dir>"
  if [ "$#" != 4 ]; then
    echo "$USAGE" >&2
    return 1
  fi

  [ "$GZOLTAR_CLI_JAR" != "" ] || die "[ERROR] GZOLTAR_CLI_JAR is not set!"
  [ -s "$GZOLTAR_CLI_JAR" ] || die "[ERROR] $GZOLTAR_CLI_JAR does not exist or it is empty!"

  local ser_file_path="$1"
  local output_dir="$2"
  local test_classpath="$3"
  local src_classes_dir="$4"

  [ -s "$ser_file_path" ] || die "$ser_file_path does not exist or it is empty!"
  mkdir -p "$output_dir" || die "Failed to create $output_dir!"

  if [ $? -ne 0 ]; then
    echo "[ERROR] _get_src_classes_dir has failed!" >&2
    return 1
  fi

  java -cp $JUNIT_JAR:$test_classpath:$GZOLTAR_CLI_JAR \
    com.gzoltar.cli.Main faultLocalizationReport \
      --buildLocation "$src_classes_dir" \
      --outputDirectory "$output_dir" \
      --dataFile "$ser_file_path" \
      --granularity "line" \
      --inclPublicMethods \
      --inclStaticConstructors \
      --inclDeprecatedMethods \
      --family "sfl" \
      --formula "ochiai" \
      --formatter "txt" || die "GZoltar faultLocalizationReport command has failed!"

  local spectra_file="$output_dir/sfl/txt/spectra.csv"
   local matrix_file="$output_dir/sfl/txt/matrix.txt"
    local tests_file="$output_dir/sfl/txt/tests.csv"

  [ -s "$spectra_file" ] || die "[ERROR] $spectra_file does not exist or it is empty!"
   [ -s "$matrix_file" ] || die "[ERROR] $matrix_file does not exist or it is empty!"
    [ -s "$tests_file" ] || die "[ERROR] $tests_file does not exist or it is empty!"

  return 0
}
####################################


# --- VARIABLES THAT SHOULD BE UPDATED [BEGIN] ---------------------------------
export GZOLTAR_CLI_JAR="$SCRIPT_DIR/lib/com.gzoltar.cli-1.7.3-SNAPSHOT-jar-with-dependencies.jar"
export GZOLTAR_AGENT_JAR="$SCRIPT_DIR/lib/com.gzoltar.agent.rt-1.7.3-SNAPSHOT-all.jar"

[ -s "$GZOLTAR_CLI_JAR" ] || die "$GZOLTAR_CLI_JAR does not exist or it is empty!"
[ -s "$GZOLTAR_AGENT_JAR" ] || die "$GZOLTAR_AGENT_JAR does not exist or it is empty!"
# --- VARIABLES THAT SHOULD BE UPDATED [END] -----------------------------------

data_dir="$1" # to save fl results
if [ -e $data_dir ]; then
    echo "$data_dir exists, delete now."
    rm -rf "$data_dir" 
    mkdir -p "$data_dir"
else
    mkdir -p "$data_dir"
fi

bug_dir="$2" # bug dir
test_classpath="$3"
test_classes_dir="$4"
src_classes_dir="$5"
src_classes_file="$6"
all_tests_file="$7"

junit_jar="$8"
export JUNIT_JAR=$junit_jar

fl_time=${data_dir}/fl_time.txt

startTime=$(date +%s)
echo "start time `date '+%Y%m%d %H%M%S'`"  >> $fl_time

echo "[INFO] Run GZoltar"
_run_gzoltar "$bug_dir" "$data_dir" "$test_classpath" "$test_classes_dir" "$src_classes_file" "$src_classes_dir" "$all_tests_file" || die "[ERROR] Execution of GZoltar has failed!"

endTime=$(date +%s)
echo "end time `date '+%Y%m%d %H%M%S'`"  >> $fl_time
repairTime=$(($endTime-$startTime))
echo -e "time cost: $repairTime s"  >> $fl_time

echo "[INFO] Generate fault localization report"
_generate_fault_localization_report "$data_dir/gzoltar.ser" "$data_dir" "$test_classpath" "$src_classes_dir" || die "[ERROR] Failed to generate a fault localization report!"

endTime=$(date +%s)
echo "end time `date '+%Y%m%d %H%M%S'`"  >> $fl_time
repairTime=$(($endTime-$startTime))
echo -e "time cost: $repairTime s\n\n"  >> $fl_time

#rm -rf "$work_dir" # Clean up

echo "DONE!"

exit 0
