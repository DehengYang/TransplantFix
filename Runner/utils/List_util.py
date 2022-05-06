from utils import File_util, Dir_util
import Config

import os

def to_lower(list):
    new_list = []
    for node in list:
        new_list.append(node.lower())
    return new_list

def get_intersection(src_list, dst_list):
    intersection = []
    for src in src_list:
        if src in dst_list:
            intersection.append(src)
    return intersection

def get_union(src_list, dst_list):
    union = []
    for src in src_list:
        union.append(src)
    for dst in dst_list:
        if dst not in union:
            union.append(dst)
    return union

def get_uniq_in_src(src_list, dst_list):
    uniq = []
    for src in src_list:
        if src not in dst_list:
            uniq.append(src)
    return uniq

def print_list(list, header=""):
    print(f"{header} list info:")
    for node in list:
        print(f"{node}")

def remove_empty_strings(list):
    new_list = []
    for node in list:
        if len(node) != 0:
            new_list.append(node)
    return new_list


if __name__ == "__main__":
    pass


