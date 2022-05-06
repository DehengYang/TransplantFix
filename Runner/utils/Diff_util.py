from os import remove
from utils import Cmd_util
from unidiff import PatchSet
from collections import OrderedDict

def get_buggy_locs(self, patch_diff_file):
        patch_set = PatchSet.from_filename(patch_diff_file)

        patch_dict = OrderedDict()
        diff_list = []

        for pacthed_file in patch_set:
            file_name = pacthed_file.source_file
            assert file_name.endswith(".java")
            assert "/" in file_name
            class_name = file_name.rsplit("/", 1)[1][:-len(".java")]

            hunks_list = []
            for hunk in pacthed_file:
                hunk_dict = OrderedDict()

                added_nos = []
                removed_nos = []
                for index in range(len(hunk)):
                    line = hunk[index]

                    if line.value.strip() == "":
                        is_empty = False
                        if index == 0:
                            if hunk[index + 1].is_context:
                                is_empty = True
                        elif index == len(hunk) - 1:
                            if hunk[index - 1].is_context:
                                is_empty = True
                        else:
                            if hunk[index +
                                    1].is_context and hunk[index -
                                                           1].is_context:
                                is_empty = True
                        if is_empty:
                            continue

                    if line.is_removed:
                        removed_nos.append(line.source_line_no)
                        continue
                    if line.is_added:
                        if index > 0:
                            prev = hunk[index - 1]
                            if prev.is_removed:
                                continue
                            if prev.is_added:
                                continue_flag = False
                                if index - 1 > 0:
                                    for prev_ind in range(index - 2, -1, -1):
                                        prev_prev = hunk[prev_ind]
                                        if prev_prev.is_removed:
                                            continue_flag = True
                                            break
                                        if prev_prev.is_context:
                                            continue_flag = False
                                            break
                                if continue_flag:
                                    continue

                            if prev.is_context:
                                added_nos.append(prev.source_line_no)

                        if index < len(hunk) - 1:
                            next = hunk[index + 1]
                            if next.is_removed:
                                added_nos.clear()
                                continue
                            if next.is_context:
                                added_nos.append(next.source_line_no)
                hunk_dict["removed"] = removed_nos
                hunk_dict["added"] = added_nos
                
                for no in removed_nos:
                    diff_list.append(f"{class_name}:{no}")
                for no in added_nos:
                    diff_list.append(f"{class_name}:{no}")

                if len(removed_nos) + len(added_nos) != 0:
                    hunks_list.append(hunk_dict)
            patch_dict[class_name] = hunks_list
        return patch_dict