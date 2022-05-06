import re


def findall(compile_string, dst_string, re_pattern = ""):
    if re_pattern == "":
        pattern = re.compile(compile_string)
    else:
        pattern = re.compile(compile_string, re_pattern)
    matches = re.findall(pattern, dst_string)
    return matches