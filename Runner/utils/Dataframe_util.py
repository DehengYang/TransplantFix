import pandas as pd
import os

from utils import File_util


def read_csv(df_path, index_col=0):
    assert os.path.exists(df_path)
    df = pd.read_csv(df_path, index_col=index_col)
    return df


def write_csv(df_path, df):
    df.to_csv(df_path)  


def save_df_str(save_path, df, index=True):
    File_util.write_str_to_file(save_path, df.to_string(index=index), False)