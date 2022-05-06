import subprocess
import logging, time

logger = logging.getLogger()


def run_cmd(cmd):
    return run_cmd_with_output(cmd)

def run_cmd_to_log(cmd, out_file, err_file):
    start_time = time.time()
    logger.info(f"cmd to run: {cmd}")

    subprocess.call(cmd, shell=True, stdout=out_file, stderr=err_file)
    
    logger.info(f"cmd execution time: {time.time() - start_time}")

def run_cmd_with_output(cmd):
    start_time = time.time()
    logger.info(f"cmd to run: {cmd}")
    p = subprocess.run(cmd,
                       shell=True,
                       stdout=subprocess.PIPE,
                       stderr=subprocess.PIPE)
    try:
        output = p.stdout.decode('utf-8')
    except UnicodeDecodeError:
        logger.warn("cmd UnicodeDecodeError")
        output = p.stdout.decode('unicode_escape')

    error = p.stderr.decode('utf-8')
    if len(error) > 0 and error.strip(
    ) != "Picked up JAVA_TOOL_OPTIONS: -Dfile.encoding=UTF8":
        logger.error(f"output error: {error}")

    if len(output) > 0:
        logger.info(f"output of this cmd: {output}")

    logger.info(f"cmd execution time: {time.time() - start_time}")
    return output

def run_cmd_with_output_without_log(cmd):
    p = subprocess.run(cmd,
                       shell=True,
                       stdout=subprocess.PIPE,
                       stderr=subprocess.PIPE)
    try:
        output = p.stdout.decode('utf-8')
    except UnicodeDecodeError:
        logger.warn("cmd UnicodeDecodeError")
        output = p.stdout.decode('unicode_escape')

    return output


def run_cmd_without_output(cmd):
    start_time = time.time()
    logger.info(f"cmd to run: {cmd}")

    return_code = subprocess.call(cmd, shell=True)

    logger.info(f"cmd execution time: {time.time() - start_time}")
    return return_code