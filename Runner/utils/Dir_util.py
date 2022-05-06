import os

def get_cur_dir(file_name):
    cur_dir = os.path.dirname(os.path.abspath(file_name))
    return cur_dir