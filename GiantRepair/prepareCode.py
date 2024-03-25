import os
import json
import subprocess
import math
import logging
from tqdm import tqdm

SUCCESSFUL_FIX_IDS_PATH_10 = '/data/PLM4APR/APR-LLM/results/baselines/{modelname}.txt'
SUCCESSFUL_FIX_IDS_PATH_20 = '/data/PLM4APR/APR-LLM/results/baselines/{modelname}_20.txt'
EXPECTATION_FIX_IDS_PATH_10 = '/data/PLM4APR/APR-LLM/results/baselines/Expectation_{modelname}.txt'
EXPECTATION_FIX_IDS_PATH_20 = '/data/PLM4APR/APR-LLM/results/baselines/Expectation_{modelname}_20.txt'

def get_bugids(model_name):
    # first get all bugs that have been generated...
    json_paths = []
    if model_name == "gpt":
        json_paths.append("/data/PLM4APR/codex_out/gpt_prompt/")
        json_paths.append("/data/PLM4APR/APR-LLM/results/gpt35_20/")
    elif model_name == "starcoder":
        json_paths.append("/data/PLM4APR/APR-LLM/results/starcoderbase/")
    elif model_name == "codellama":
        json_paths.append("/data/PLM4APR/APR-LLM/results/CodeLlama-7b-hf/")
    elif model_name == "llama":
        json_paths.append("/data/PLM4APR/APR-LLM/results/Llama-2-13b-hf_200/")
    elif model_name == "growing_bugs":
        json_paths.append("/data/PLM4APR/APR-LLM/results/starcoderbase_growing/")
    
    all_bug_ids = []
    for path_str in json_paths:
        allfiles = os.listdir(path_str)
        for filestr in allfiles:
            if os.path.isfile(os.path.join(path_str, filestr)) and filestr.endswith(".json") and "-" in filestr:
                bugid = filestr.split(".json")[0]
                all_bug_ids.append(bugid)
    
    # then, get all fixed bugs, they do not need rerun...
    already_fix_bugs_paths = [SUCCESSFUL_FIX_IDS_PATH_10, SUCCESSFUL_FIX_IDS_PATH_20, EXPECTATION_FIX_IDS_PATH_10, EXPECTATION_FIX_IDS_PATH_20]
    all_fixed_bug_ids = []
    for pathDef in already_fix_bugs_paths:
        path_str = pathDef.format(modelname=model_name)
        
        if not os.path.exists(path_str):
            continue

        lines = open(path_str, 'r').readlines()
        for line in lines:
            line = line.strip()
            if len(line) == 0:
                continue
            proj, idnum = line.split("_")
            all_fixed_bug_ids.append(proj+"-"+idnum)
    if model_name == "growing_bugs":
        ret = {}
    else:
        ret = {
            "Chart":[], 
            "Closure": [], 
            "Lang": [], 
            "Math": [], 
            "Time": [], 
            "Cli": [], 
            "Codec": [], 
            "Collections": [], 
            "Compress": [], 
            "Csv": [], 
            "Gson": [], 
            "JacksonCore": [], 
            "JacksonDatabind": [], 
            "JacksonXml": [], 
            "Jsoup": [], 
            "JxPath": [], 
            "Mockito": []
        }
    for bugid in all_bug_ids:
        if bugid in all_fixed_bug_ids:
            continue
        # print(bugid)
        proj, idnum = bugid.split("-")
        if proj not in ret.keys():
            ret[proj] = []
            ret[proj].append(int(idnum))
        else:
            ret[proj].append(int(idnum))
    
    for key in ret.keys():
        ret[key].sort()

    # print(ret)
    return ret

def diff_models(base_bug, other_bugs):
    need_check = {}
    for bug in other_bugs:
        for key in bug.keys():
            for idnum in bug[key]:
                if idnum not in base_bug[key]:
                    if key not in need_check.keys():
                        need_check[key] = [idnum]
                    else:
                        if idnum not in need_check[key]:
                            need_check[key].append(idnum)
    for key in need_check.keys():
        need_check[key].sort()
    print(need_check)
    return need_check

# def diff_models(base_bug, other_bugs, x):
#     for bug in other_bugs:
#         for key in bug.keys():
#             for idnum in bug[key]:
#                 if idnum not in base_bug[key]:
#                     base_bug[key].append(idnum)
#     for key in base_bug.keys():
#         base_bug[key] = list(set(base_bug[key]))
#         base_bug[key].sort()
#     counter = 0
#     for key in base_bug.keys():
#         counter += len(base_bug[key])
#         print("{k} : {amount}\n".format(k=key, amount=len(base_bug[key])))
#     print(counter)
   
def checkExpressAPR(bugs_dict):
    CheckOutCMD = "cd /data/PLM4APR/ExpressAPR " + "&& ./expapr-cli init -i defects4j -b {bugid} -w /data/PLM4APR/tmp/exapr/{proj}/{proj}_{idnum}_buggy/ -j 20 -d trivial --reuse-workdir"
    FAILED_BUGS = []
    # check_list = ["Csv", "Gson", "JacksonCore", "JacksonDatabind", "JacksonXml", "Jsoup", "JxPath", "Mockito"]
    # check_list = ["Lang", "Math", "Time"]
    # check_list = ["Math", "Closure"] # server 5
    # check_list = ["Closure"] # server 25
    # check_list = ["Chart", "Lang"] # server 12
    check_list = ["Mockito"]
    for key in  bugs_dict.keys():
        if key not in check_list:
            continue
        for idnum in bugs_dict[key]:
            # if key=="Closure" and (idnum < 100 or idnum > 133):
            #     continue
            if os.path.exists("/data/PLM4APR/tmp/exapr/{proj}/{proj}_{idnum}_buggy/repo-buggy-19".format(proj=key.lower(), idnum=idnum)):
                continue
            cmd = CheckOutCMD.format(bugid=key+"-"+str(idnum), proj=key.lower(), idnum=idnum)    
            print("Checking out bugid: {}".format(key+"-"+str(idnum)))
            result = subprocess.run(cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
            stdout_output = result.stdout
            stderr_output = result.stderr
            return_code = result.returncode
            if return_code != 0:
                FAILED_BUGS.append(key+"-"+str(idnum))
                logging.error(stdout_output)
                logging.error(stderr_output)
    print("CHECK EXPRESSAPR FAILED:{}".format(FAILED_BUGS))

def extract_jsons(model_name):
    json_paths = []
    if model_name == "gpt":
        json_paths.append("/data/PLM4APR/codex_out/gpt_prompt/")
        json_paths.append("/data/PLM4APR/APR-LLM/results/gpt35_20/")
    elif model_name == "starcoder":
        json_paths.append("/data/PLM4APR/APR-LLM/results/starcoderbase/")
    elif model_name == "codellama":
        json_paths.append("/data/PLM4APR/APR-LLM/results/CodeLlama-7b-hf/")
    elif model_name == "llama":
        json_paths.append("/data/PLM4APR/APR-LLM/results/Llama-2-13b-hf_200/")
    elif model_name == "growing_bugs":
        json_paths.append("/data/PLM4APR/APR-LLM/results/starcoderbase_growing/")
    
    all_jsons_path = []
    for path_str in json_paths:
        allfiles = os.listdir(path_str)
        for filestr in allfiles:
            if os.path.isfile(os.path.join(path_str, filestr)) and filestr.endswith(".json") and "-" in filestr:
                all_jsons_path.append(os.path.join(path_str, filestr))
    
    # extract patches from jsons
    dst_dir = "/data/PLM4APR/codex_out/200_patches_{modelname}_all/".format(modelname = model_name)
    for json_path in tqdm(all_jsons_path):
        bugid = json_path.split("/")[-1].split(".json")[0]
        json_content = json.load(open(json_path, 'r'))
        for patch in json_content:
            patch_content = patch["output"]
            proj, idnum = bugid.split("-")
            target_path = dst_dir + proj.lower() + "_" + idnum + "/"
            target_file = target_path + str(json_content.index(patch))+".java"
            if os.path.exists(target_path) == False:
                os.makedirs(target_path)
            with open(target_file, "w") as f:
                f.write("public class tmp {\n" + patch_content + "\n}")
            f.close()
    return 

def extract_bugs(model_name):
    bugids = get_bugids(model_name)
    if model_name == "growing_bugs":
        json_object = json.load(open("/data/PLM4APR/d4j-info/growing_bugs_single_function.json", "r"))
    else:
        json_object = json.load(open("/data/PLM4APR/d4j-info/single_function_repair.json", 'r'))
    all_bugids = []
    for key in bugids:
        for idnum in bugids[key]:
            all_bugids.append(key+"-"+str(idnum))
    dst_dir = "/data/PLM4APR/codex_out/potential_bugs_{modelName}_all/{bugid}/"
    for bugid in tqdm(all_bugids):
        buggy_str = json_object[bugid]["buggy"]
        lower_bugid = bugid.lower().split("-")[0] + "_" + bugid.split("-")[1]
        dst_path = dst_dir.format(modelName=model_name, bugid= lower_bugid)
        if os.path.exists(dst_path) is False:
            os.makedirs(dst_path)
        with open(dst_path + "buggy.java", 'w') as f:
            f.write("public class tmp {\n" + buggy_str + "\n}")
        f.close()
        code_formatter(dst_path)

def code_formatter(dst_path):
    """
    call CodeFormatter to format the code
    """
    command = "java -jar CodeFormatter.jar remote {path}".format(path=dst_path)
    result = subprocess.run(command,shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    if result.returncode != 0:
        print(result.stderr)
    return result.returncode

def deduplicated(model_name):
    dstPath = "/data/PLM4APR/codex_out/200_patches_{modelname}_all/".format(modelname = model_name)
    dirs = os.listdir(dstPath)
    bugs = [item for item in dirs if os.path.isdir(dstPath+item)] # bugs: [chart_1, closure_xx ...]

    patch_counter = 0
    for bug in tqdm(bugs):
        print(bug)
        files = os.listdir(dstPath+bug)
        files = [file for file in files if file.endswith(".java")]
        file_amount = len(files)
        code_set = []
        id_set = []
        for fileid in range(file_amount):
            patch_file_path = dstPath + bug + "/" + str(fileid) +".java"
            if code_formatter(patch_file_path) == 0:
                code = open(patch_file_path, 'r').readlines()
                if code not in code_set:
                    code_set.append(code)
                    id_set.append(fileid)
            else:
                continue
        patch_counter += len(id_set)
        with open ('./shrink.txt', 'a') as f:
            f.write("{before} -> {now}\n".format(before=file_amount, now=len(id_set)))
        f.close()
        for fileid in range(file_amount):
            if fileid not in id_set:
                os.remove(dstPath + bug + "/" + str(fileid) +".java")
            else:
                os.rename(dstPath + bug + "/" + str(fileid) +".java", dstPath + bug + "/" + str(id_set.index(fileid)) +".java")    
        # break
    print("{model} has {pn} patch for {bn} bugs, average:{avg}".format(model=model_name, pn = patch_counter, bn=len(bugs), avg=(patch_counter * 1.0)/len(bugs)))


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

def rank_by_diffLines(bugids):
    model = "gpt35"
    BUGGYFILEPATH = '/data/PLM4APR/codex_out/potential_bugs_{modelName}_all/{bugid}/buggy.java'
    PATCHESPath = '/data/PLM4APR/codex_out/200_patches_{modelName}_all/{bugid}/'
    for bugid in bugids:
        buggyfilePath = BUGGYFILEPATH.format(modelName = model, bugid = bugid.lower())
        patchesPath = PATCHESPath.format(modelName = model, bugid= bugid.lower())
        patch_files = [file for file in os.listdir(patchesPath) if file.endswith(".java")]
        patch_files = [str(i)+".java" for i in range(0, len(patch_files))]
        diff_lines_amount = []
        if len(patch_files) < 60:
            continue
        for patch_file in patch_files:
            cmd = "git diff {buggy} {patch}".format(buggy=buggyfilePath, patch=patchesPath+ patch_file)
            result = subprocess.run(cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
            if result.returncode == -1:
                print("Failed to run git diff on bugid: " + bugid +"return code:"+str(result.returncode))
            else:
                diff_lines_amount.append(parse_diff_lines(result.stdout.split("\n")))
        # print("{bugid}: {dis}".format(bugid=bugid, dis=diff_lines_amount))
        avg=sum(diff_lines_amount)/len(diff_lines_amount)
        counter = 0
        for item in diff_lines_amount:
            if item > avg:
                counter += 1
        print("{bugid} average is:{avg}, {amount}/{all} can be delete".format(bugid = bugid, avg=avg, amount = counter, all=len(diff_lines_amount)))

        

if __name__ == "__main__":
    # prepare ExpressAPR environment 
    # to_check_bugs = get_bugids("gpt")

    # starcoder_check_bugs = get_bugids("starcoder")
    # codellama_check_bugs = get_bugids("codellama")
    # llama_check_bugs = get_bugids("llama")
    # need_check = diff_models(to_check_bugs, [starcoder_check_bugs, codellama_check_bugs, llama_check_bugs])
    # checkExpressAPR(to_check_bugs)

    # trying to filter out uncessary patches
    # format_bugs = []
    # for key in to_check_bugs.keys():
    #     for idnum in to_check_bugs[key]:
    #         format_bugs.append(key+"_"+str(idnum))
    # rank_by_diffLines(bugids=format_bugs)


    #====================Following are json extractions and deduplicate process====================
    extract_bugs("growing_bugs")
    extract_jsons("growing_bugs")
    deduplicated("growing_bugs")
