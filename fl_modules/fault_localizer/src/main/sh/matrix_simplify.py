# -*- coding: utf-8 -*-
"""
Created on Sun Feb 23 17:06:40 2020

This py script is to:
    1) simplify the ochiai.ranking.csv by filtering out statements of which the suspiciousness is less than 
        or equal to 0;
    2) get the indices/positions of these non-suspicious statements in the spectra.csv, and discard them;
    3) simplify the matrix.txt by deleting the columns corresponding to these non-suspicious statements.
"""

import pandas as pd
import os
import sys
import time
import numpy as np
import csv

# constants
log_path = "log.txt"
fl_dir = "sfl/txt/"

# read ochiai result
def simplify_fl_result(directory = fl_dir):
    '''
        1) simplify ochiai result
    '''
#    with open(path, encoding = 'utf-8', mode = 'r') as f:
#        f_csv = csv.reader(f)   # TODO: discard the first line "name;suspiciousness_value"
#        for row in f_csv:
    # refer to: [python3£ºcsvµÄ¶ÁÐ´ (in Chinese)](https://blog.csdn.net/katyusha1/article/details/81606175)
    
    # read data
    # refer to: [dataframe É¾³ýÐÐ²Ù×÷--É¾³ýÌØ¶¨ÖµµÄÐÐ (in Chinese)](https://blog.csdn.net/tcc2430/article/details/100879505)
    # [É¾³ýDataFrameÖÐÌØ¶¨Ìõ¼þµÄÐÐ/ÁÐ (in Chinese)](https://www.cnblogs.com/dataAnalysis/p/9487774.html)
    start = time.time()
    ochiai_path = directory + "ochiai.ranking.csv"
    if not os.path.exists(ochiai_path):
        print("{} does not exist. Please make sure the fl result path exists. Exit now.".format(ochiai_path))
        sys.exit()
    ochi_df = pd.read_csv(ochiai_path, delimiter = ';')  
    end = time.time()
    print("1-1) time cost: {}".format(end - start))
    
    # save filtered csv
    # refer to: [pandas¶ÁÈ¡csvÎÄ¼þµÄ²Ù×÷ (in Chinese)](https://blog.csdn.net/Bin_1022/article/details/83413470)
    start = time.time()
    df_faulty = ochi_df[ochi_df.suspiciousness_value > 0]
    df_faulty.to_csv(directory + "ochiai.ranking.faulty.csv", index = None, sep = ';')
    end = time.time()
    print("1-2) time cost: {}".format(end - start))
    
    start = time.time()
    df_non_faulty = ochi_df[ochi_df.suspiciousness_value <= 0]
    df_non_faulty.to_csv(directory + "ochiai.ranking.nonFaulty.csv", index = None, sep = ';') # quoting=0
    end = time.time()
    print("1-3) time cost: {}".format(end - start))
    
    
    '''
        2) simplify the spectra
    '''
    start = time.time()
    spectra_path = directory + "spectra.csv"
    spec_df = pd.read_csv(spectra_path, delimiter = ";")  
    
    faulty_index_list = []  # should save faulty index rather than non-faulty, as the latter size is too large.

    for index, row in df_faulty.iterrows():
        cur_stmt = row["name"]
        index_list = spec_df[spec_df.name == cur_stmt].index.tolist()
        if len(index_list) != 1:
            print("{} index size is larger than 1. Exit now.".format(cur_stmt))
            sys.exit()
        faulty_index_list.append(index_list[0])
    end = time.time()
    print("2-1) time cost: {}".format(end - start))
    
    start = time.time()
    # sort faulty_index_list
    faulty_index_list.sort(reverse = False)
    # refer to: [pandas.dataframe°´ÐÐË÷Òý±í´ïÊ½Ñ¡È¡·½·¨ (in Chinese)][https://www.jb51.net/article/149779.htm]
    faulty_spec_df = spec_df.iloc[faulty_index_list]
    faulty_spec_df.to_csv(directory + "spectra.faulty.csv", index=None, sep = ';')
    end = time.time()
    print("2-2) time cost: {}".format(end - start))
    
    '''
        3) simplify the matrix
    '''
    start = time.time()
    tests_path = directory + "tests.csv"
    # cannot set as "," that will cause ParserError: Error tokenizing data. C error: Expected 4 fields in line 66, saw 5
    # tests_df = pd.read_csv(tests_path, delimiter = ",") 
    
    matrix_path = directory + "matrix.txt"
    filtered_matrix_path = directory + "filtered_matrix.txt"
    with open(filtered_matrix_path, encoding = 'utf-8', mode = 'w') as write_f:
        with open(matrix_path, encoding = 'utf-8', mode = 'r') as f:
            ind = 0
            for line in f: # refer to [How can I read large text files in Python, line by line, without loading it into memory?](https://stackoverflow.com/questions/6475328/how-can-i-read-large-text-files-in-python-line-by-line-without-loading-it-into)
                # refer to: [Access multiple elements of list knowing their index](https://stackoverflow.com/questions/18272160/access-multiple-elements-of-list-knowing-their-index)
                cover_list = np.array(line.strip().split(" ")) # seems strip() is not necessary, as each line seems has no whitespace at the beginning and the end
                result = cover_list[-1] # if successful
                faulty_cover_list = cover_list[faulty_index_list]
                
                processed_line = ""
                for bit in faulty_cover_list:
                    processed_line = processed_line + bit + " "
                #processed_line = np.array_str(faulty_cover_list)[1:-1] + " " + result
                processed_line = processed_line + result
                write_f.write(processed_line.strip() + "\n") # write to file
                
                if result == "-":
                    print(ind)
                    # refer to: [How to print a specific row of a pandas DataFrame?](https://stackoverflow.com/questions/43772362/how-to-print-a-specific-row-of-a-pandas-dataframe)
                    # print("Index: {}, failed case info: {}".format(ind, tests_df.iloc[[ind]]))  # print failed test case name
                    
                    #print("Index: {}, failed case info: name: {}, outcome: {}, strack trace: {}"
                    #      .format(ind, tests_df.iloc[ind, 0], tests_df.iloc[ind, 1], 
                    #              tests_df.iloc[ind, 3]))
                    
                    #print("Index: {}, failed case info: {}".format(ind, tests_df.iloc[ind, 0]))
                    #log("Index: {}, failed case info: {}".format(ind, tests_df.iloc[ind, 0]))
                    
                ind += 1 # increment
    end = time.time()
    print("3-1) time cost: {}".format(end - start))            
                
    return df_faulty, df_non_faulty, faulty_index_list #, tests_df

# save some info into log.txt
def log(string, path = log_path):
    with open(path, encoding = 'utf-8', mode = 'a+') as write_f:
        write_f.write(string + "\n")

def test_empty_file():
    pass

if __name__ == "__main__":
    if os.path.exists(log_path):
        os.remove(log_path)
    df_faulty, df_non_faulty, faulty_index_list = simplify_fl_result() #, tests_df
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    
