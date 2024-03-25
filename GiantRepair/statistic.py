# this script is used to calculate the statistic of the different 
import os
import json
import subprocess
import math
# import matplotlib.pyplot as plt

def get_bugids(file):
    """
    get the valuable bugs from human checked txt
    """
    bugids = []
    target_index = []
    file_content = open(file, 'r').readlines()
    for line in file_content:
        if len(line.strip()) == 0:
            continue
        infos = line.strip().split("_")
        assert len(infos) == 3
        bugids.append(infos[0]+"-"+infos[1])
        target_index.append(int(infos[2]))
    return bugids, target_index

def extract_from_json(jsonPath, bugid, dstPath):
    """
    extract the 200 patches to specific folder
    """
    json_file = os.path.join(jsonPath, bugid+".json")
    json_content = json.load(open(json_file, 'r'))
    for patch in json_content:
        patch_content = patch["output"]
        proj, idnum = bugid.split("-")
        target_path = dstPath + proj.lower() + "_" + idnum + "/"
        target_file = target_path + str(json_content.index(patch))+".java"
        if os.path.exists(target_path) == False:
            os.makedirs(target_path)
        with open(target_file, "w") as f:
            f.write("public class tmp {\n" + patch_content + "\n}")
        f.close()
    return

def code_formatter(dst_path):
    """
    call CodeFormatter to format the code
    """
    command = "java -jar CodeFormatter.jar remote {path}".format(path=dst_path)
    os.system(command)
    return

def get_actions_number(bugid):
    """
    get the number of actions in each patch
    """
    cmd = "java -jar GenPat.jar statistic -d4j {bugid} -d4jhome /data/PLM4APR/tmp/defects4j_buggy/"
    result = subprocess.run(cmd.format(bugid=bugid), shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    if result.returncode != 0:
        print("Failed to run APR tool on bugid: " + bugid)
        # print(result.stdout)
    return

def parse_diff_lines(outcome):
    add_lines_counter = 0
    del_liens_counter = 0
    # print(outcome)
    for line in outcome:
        if line.startswith("+") and line.startswith("+++") == False:
            add_lines_counter += 1
        elif line.startswith("-") and line.startswith("---") == False:
            del_liens_counter += 1
        else:
            continue
    # return add_lines_counter + del_liens_counter
    return max(0, add_lines_counter - del_liens_counter)

def get_git_diff_lines():
    
    bug_ids, target_indexes = get_bugids("../codex_out/valuable_patches_gpt35_new.txt")
    buggy_func_path = "../codex_out/potential_bugs_gpt35_real/"
    patch_func_path = "../codex_out/200_patches_gpt35/"
    
    # bug_ids, target_indexes = get_bugids("../codex_out/valuable_patches_starcoder.txt")
    # buggy_func_path = "../codex_out/potential_bugs_starcoder_real/"
    # patch_func_path = "../codex_out/200_patches_starcoder/"

    # bug_ids, target_indexes = get_bugids("../codex_out/valuable_patches_codellama.txt")
    # buggy_func_path = "../codex_out/potential_bugs_codellama_real/"
    # patch_func_path = "../codex_out/200_patches_codellama/"

    # bug_ids, target_indexes = get_bugids("../codex_out/valuable_patches_llama.txt")
    # buggy_func_path = "../codex_out/potential_bugs_llama_real/"
    # patch_func_path = "../codex_out/200_patches_llama/"
    
    target_patch_diff_amount = []
    top_lines_amount = []
    average_lines_amount = []
    ranks = []
    amount= []
    for bug in bug_ids:
        repo, idnum = bug.split("-")
        buggy_file = buggy_func_path + repo.lower() + "_" + idnum + "/buggy.java"
        patch_dir  = patch_func_path + repo.lower() + "_" + idnum + "/"
        patch_files = [file for file in os.listdir(patch_dir) if file.endswith(".java")]
        patch_files = [str(i)+".java" for i in range(0, len(patch_files))]
        diff_lines_amount = []
        for patch_file in patch_files:
            # print("Current patch is {}".format(patch_file))
            cmd = "git diff {buggy} {patch}".format(buggy=buggy_file, patch=patch_dir + patch_file)
            result = subprocess.run(cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
            if result.returncode == -1:
                print("Failed to run git diff on bugid: " + bug +"return code:"+str(result.returncode))
            else:
                diff_lines_amount.append(parse_diff_lines(result.stdout.split("\n")))
        
        # first get the diff amount of target patch
        target_diff_amount = diff_lines_amount[target_indexes[bug_ids.index(bug)]]
        # print("Current bug is {}, diff lines are:{}, index: {}".format(bug, target_diff_amount, target_indexes[bug_ids.index(bug)]))
        ranks.append(sorted(diff_lines_amount).index(target_diff_amount))
        target_patch_diff_amount.append(target_diff_amount)
        top_lines_amount.append(sorted(diff_lines_amount)[-1])
        average_lines_amount.append(sum(diff_lines_amount)/len(diff_lines_amount))
        amount.append(len(diff_lines_amount))
    for i in range(0, len(ranks)):
        print(bug_ids[i] + ": from" +str(target_indexes[i])+"->" + str(ranks[i])+"/"+str(amount[i]) +";"+str(target_patch_diff_amount[i])+"/"+str(top_lines_amount[i])+"; average:"+str(average_lines_amount[i]))
        # print(bug_ids[i] + ": from" +str(target_indexes[i])+"->" + str(ranks[i])+"/"+str(amount[i]) +";")
    return

def levenshtein_distance(s1, s2):
    if len(s1) < len(s2):
        return levenshtein_distance(s2, s1)

    # len(s1) >= len(s2)
    if len(s2) == 0:
        return len(s1)

    previous_row = range(len(s2) + 1)
    for i, c1 in enumerate(s1):
        current_row = [i + 1]
        for j, c2 in enumerate(s2):
            insertions = previous_row[j + 1] + 1
            deletions = current_row[j] + 1
            substitutions = previous_row[j] + (c1 != c2)
            current_row.append(min(insertions, deletions, substitutions))
        previous_row = current_row
    
    return previous_row[-1]/len(s1)

def get_edit_distance():
    # bug_ids, target_indexes = get_bugids("../codex_out/valuable_patches_starcoder.txt")
    # buggy_func_path = "../codex_out/potential_bugs_starcoder_real/"
    # patch_func_path = "../codex_out/200_patches_starcoder/"
    
    # bug_ids, target_indexes = get_bugids("../codex_out/valuable_patches_gpt35_new.txt")
    # buggy_func_path = "../codex_out/potential_bugs_gpt35_real/"
    # patch_func_path = "../codex_out/200_patches_gpt35/"
    
    bug_ids, target_indexes = get_bugids("../codex_out/valuable_patches_codellama.txt")
    buggy_func_path = "../codex_out/potential_bugs_codellama_real/"
    patch_func_path = "../codex_out/200_patches_codellama/"
    
    target_edit_distances = []
    top_edit_distances = []
    average_edit_distance = []
    ranks = []
    patch_amount= []
    for bug in bug_ids:
        repo, idnum = bug.split("-")
        buggy_file = buggy_func_path + repo.lower() + "_" + idnum + "/buggy.java"
        patch_dir  = patch_func_path + repo.lower() + "_" + idnum + "/"
        patch_files = [file for file in os.listdir(patch_dir) if file.endswith(".java")]
        patch_files = [str(i)+".java" for i in range(0, len(patch_files))]
        edit_distances = []
        for patch_file in patch_files:
            # print("Current patch is {}".format(patch_file))
            buggy_content = open(buggy_file, 'r').readlines()
            patch_content = open(patch_dir + patch_file, 'r').readlines()
            edit_distances.append(levenshtein_distance(buggy_content, patch_content))
        # first get the diff amount of target patch
        target_edit_distance = edit_distances[target_indexes[bug_ids.index(bug)]]
        # print("Current bug is {}, diff lines are:{}, index: {}".format(bug, target_diff_amount, target_indexes[bug_ids.index(bug)]))
        
        ranks.append(sorted(edit_distances).index(target_edit_distance))
        target_edit_distances.append(target_edit_distance)
        top_edit_distances.append(sorted(edit_distances)[-1])
        average_edit_distance.append(sum(edit_distances)/len(edit_distances))
        patch_amount.append(len(edit_distances))
    for i in range(0, len(ranks)):
        print(bug_ids[i] + ": from"+str(target_indexes[i])+"->" + str(ranks[i])+"/"+str(patch_amount[i]) +";"+str(target_edit_distances[i])+"/"+str(top_edit_distances[i])+"; average:"+str(average_edit_distance[i]))
        # print(bug_ids[i] + ": from"+str(target_indexes[i])+"->" + str(ranks[i])+"/"+str(patch_amount[i]) +";")

    return

def deduplicate(dstPath, valuable_patches_path):
    print(dst_path)
    
    valuable_patches_file = open(valuable_patches_path, 'r').readlines()
    valuable_patches = {}
    for line in valuable_patches_file:
        if(len(line.strip()) == 0):
            continue
        proj, idnum, rank = line.strip().split("_")
        proj = proj.lower()
        valuable_patches[proj+"_"+idnum] = int(rank)
    
    dir = os.listdir(dstPath)
    bugs = [item for item in dir if os.path.isdir(dstPath+item)]
    for bug in bugs:
        list_ids = []
        set_strs = []
        deduplicate_dict = {}
        files = os.listdir(dstPath+bug)
        files = [file for file in files if file.endswith(".java")]
        counter = len(files)
        for i in range(0, counter):
            code = open(dstPath+bug+"/"+str(i)+".java", 'r').readlines()
            if code in set_strs:
                deduplicate_dict[i] = list_ids[set_strs.index(code)]
                continue
            else:
                deduplicate_dict[i] = i
                set_strs.append(code)
                list_ids.append(i)
        
        print(bug+" compress from {all} to {now}".format(all=counter, now=len(list_ids)))
        # print(deduplicate_dict)
        
        if deduplicate_dict[valuable_patches[bug]] == valuable_patches[bug]:
            valuable_patches[bug] = list_ids.index(valuable_patches[bug])
        else:
            valuable_patches[bug] = list_ids.index(deduplicate_dict[valuable_patches[bug]])
        
        for key in deduplicate_dict.keys():
            if deduplicate_dict[key] != key:
                os.remove(dstPath+bug+"/"+str(key)+".java")
            else:
                os.rename(dstPath+bug+"/"+str(key)+".java", dstPath+bug+"/"+str(list_ids.index(key))+".java")
    print(valuable_patches)
    with open(valuable_patches_path, 'w') as f:
        for key in valuable_patches.keys():
            f.write(key.capitalize()+"_"+str(valuable_patches[key])+"\n")
   
def make_test_dirs(valuable_patches_path, all_patch_dir, modelName):
    d4j_json = "../d4j-info/single_function_repair.json"
    bugs_dict = json.load(open(d4j_json, 'r'))
    bug_ids, target_indexes = get_bugids(valuable_patches_path)
    dst_potential_bugs_dir = "/data/PLM4APR/codex_out/potential_bugs_"+modelName+"_real/"
    dst_valuable_patches_dir = "/data/PLM4APR/codex_out/valuable_patches_"+modelName+"_real/"
    for bug in bug_ids:
        repo, idnum = bug.split("-")
        patch_dir  = all_patch_dir + repo.lower() + "_" + idnum + "/"
        indexNum = target_indexes[bug_ids.index(bug)]
        dst_bug_dir = dst_potential_bugs_dir + repo.lower() + "_" + idnum + "/"
        dst_patch_dir = dst_valuable_patches_dir + repo.lower() + "_" + idnum + "/"
        if os.path.exists(dst_bug_dir) == False:
            os.makedirs(dst_bug_dir)
        if os.path.exists(dst_patch_dir) == False:
            os.makedirs(dst_patch_dir)
        os.system("cp "+patch_dir+str(indexNum)+".java "+dst_patch_dir+"patch.java")
        if repo == "Jacksoncore":
            repo = "JacksonCore"
            bug_content = bugs_dict[repo+"-"+idnum]["buggy"]
        elif repo == "Jacksondatabind":
            repo = "JacksonDatabind"
            bug_content = bugs_dict[repo+"-"+idnum]["buggy"]
        elif repo == "Jacksonxml":
            repo = "JacksonXml"
            bug_content = bugs_dict[repo+"-"+idnum]["buggy"]
        elif repo == "Jxpath":
            repo = "JxPath"
            bug_content = bugs_dict[repo+"-"+idnum]["buggy"]
        else:
            bug_content = bugs_dict[repo.capitalize()+"-"+idnum]["buggy"]
        with open(dst_bug_dir+"buggy.java", 'w') as f:
            f.write("public class tmp {\n" + bug_content + "\n}")
        f.close()
        code_formatter(dst_bug_dir)
    pass 

if __name__ == "__main__":
    # bug_ids_path = "../codex_out/valuable_patches_llama_20.txt"
    # bug_ids, target_indexes = get_bugids(bug_ids_path)
    
    # Extract json files 
    # jsons_path = "../codex_out/gpt_prompt/"
    # dst_path   = "../codex_out/200_patches_gpt35/"
    # jsons_path = "../APR-LLM/results/Llama-2-13b-hf_200/"
    # dst_path = "../codex_out/200_patches_llama/"
    
    # jsons_path = "../APR-LLM/results/Llama-2-13b-hf_200"
    # dst_path = "../codex_out/200_patches_llama_20/"
    
    # if os.path.exists(dst_path) == False:
    #     for bug in bug_ids:
    #         # first extract from json to java files
    #         extract_from_json(jsons_path, bug, dst_path)
    #         # then format the code
    #         code_formatter(dst_path+bug.split("-")[0].lower()+"_"+bug.split("-")[1]+"/")

    # count the unique patches
    # dst_path = "../codex_out/200_patches_llama_20/"
    # valuable_patches_path = "../codex_out/valuable_patches_llama_20.txt"
    # deduplicate(dst_path, valuable_patches_path)
    
    # make the test dirs
    # dst_path = "../codex_out/200_patches_llama_20/"
    # valuable_patches_path = "../codex_out/valuable_patches_llama_20.txt"
    # make_test_dirs(valuable_patches_path, dst_path, "llama")

    # Feature 1:
    # caculate the number of changes
    # for bug in bug_ids:
    #     print("Current bug is {}".format(bug))
    #     get_actions_number(bug.split("-")[0].lower()+"_"+bug.split("-")[1])
    
    # Feature 2: git diff lines
    get_git_diff_lines()
    
    # Feature 3: edit distance lines.
    # get_edit_distance()
    