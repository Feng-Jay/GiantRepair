import os
import subprocess
import argparse

result_on_gpt35 ={
    "closure":[15, 33, 55, 114, 118, 161],
    "math":[10,25,58,75]
}
result_on_starcoder ={
    "closure":[5, 15, 19, 33, 36, 55, 161],
    "math":[10, 27, 48, 94, 106],
}

result_on_codellama={
    "closure": [10, 15, 20, 33, 101, 113, 161],
    "lang":[14, 57]
}

result_on_llama={
    "closure": [5, 15],
    "lang":[55],
    "math":[58,85],
}

result_on_gpt35_20 = {
    "compress": [23, 27, 31],
    "codec": [4],
    "csv": [9],
    "jacksoncore":[20],
    "jacksondatabind":[51],
    "jacksonxml":[5],
    "jxpath":[21],
}

result_on_starcoder_20 = {
    "cli":[32],
    "codec":[4],
    "compress": [1, 27, 31, 46],
    "jacksoncore":[3, 25],
    "jacksondatabind":[51],
    "jacksonxml":[5],
    "jsoup": [39]
}

result_on_codellama_20 = {
    "cli":[32],
    "codec":[4],
    "compress": [1],
    "jacksoncore":[5, 11],
    "jacksondatabind":[46, 51, 102],
    "jacksonxml":[5],
    "jsoup": [24, 33]
}

result_on_llama_20 = {
    "cli":[32],
    "codec":[4],
    "compress": [1, 31],
    "jacksoncore":[7, 11],
    "jacksondatabind":[24]
    # "jacksonxml":[5]
}

def get_bug_ids():
    ground_truth_dir = "./resources/groundTruth/gpt35/"
    bugids = os.listdir(ground_truth_dir)
    bugids = [bugid.split(".")[0] for bugid in bugids if bugid.endswith(".txt")]
    bug_infos = {}
    for bug in bugids:
        repo, idnum = bug.split("_")
        idnum = (int)(idnum)
        if bug_infos.get(repo) is None:
            bug_infos[repo] = [idnum]
        else:
            bug_infos[repo].append(idnum)
    for key in bug_infos.keys():
        bug_infos[key].sort()
    bugids = []
    for key in bug_infos.keys():
        for idnum in bug_infos[key]:
            bugids.append("{}_{}".format(key, idnum))
    print(bugids)

def get_bug_ids(llmName, version):
    if llmName == "gpt35":
        if version == "10":
            result_dict = result_on_gpt35
        elif version == "20":
            result_dict = result_on_gpt35_20
    elif llmName == "starcoder":
        if version == "10":
            result_dict = result_on_starcoder
        elif version == "20":
            result_dict = result_on_starcoder_20
    elif llmName == "codellama":
        if version == "10":
            result_dict = result_on_codellama
        elif version == "20":
            result_dict = result_on_codellama_20
    elif llmName == "llama":
        if version == "10":
            result_dict = result_on_llama
        elif version == "20":
            result_dict = result_on_llama_20
    bugids = []
    for key in result_dict.keys():
        for idnum in result_dict[key]:
            bugids.append("{}_{}".format(key, idnum))
    with open("fix_success_bugs.txt", "w") as f: 
        f.write("{} outcomes:\n".format(llmName))
    return bugids

def run_apr_tools(llmName, version):
    
    bugids = get_bug_ids(llmName, version)
    
    RUN_APR_TOOL_CMD = "java -jar GenPat.jar repair -d4j {bugid} -d4jhome /data/PLM4APR/tmp/defects4j_buggy/ -modelname {modelName}"
    CORRECT_FIXES_ORACLE = "Correct fixed!!! Patch Rank:"

    RUN_SUCCESS_BUGS = []
    FIX_PATCH_RANKS = []
    RUN_FAILED_BUGS = []
    FIX_SUCCESS_BUGS = []
    FIX_FAILED_BUGS = []
    for bugid in bugids:
        print("Current fix is {}".format(bugid))
        result = subprocess.run(RUN_APR_TOOL_CMD.format(bugid=bugid, modelName=llmName), shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        if result.returncode != -1:
            RUN_SUCCESS_BUGS.append(bugid)
            out_info = result.stdout
            if CORRECT_FIXES_ORACLE in out_info:
                out_info = out_info.split(CORRECT_FIXES_ORACLE)[1]
                FIX_SUCCESS_BUGS.append(bugid)
                rank_info = out_info.split("\n")[0].strip()
                FIX_PATCH_RANKS.append(rank_info)
                print(out_info)
            else:
                #print(out_info)
                print("Failed to fix bugid: " + bugid)
                FIX_FAILED_BUGS.append(bugid)
        else:
            RUN_FAILED_BUGS.append(bugid)
            print("Failed to run APR tool on bugid: " + bugid)
            print(result.stdout)

    print("RUN_SUCCESS_BUGS: " + str(RUN_SUCCESS_BUGS))
    print("RUN_FAILED_BUGS: " + str(RUN_FAILED_BUGS))
    print("FIX_SUCCESS_BUGS: " + str(FIX_SUCCESS_BUGS))
    print("FIX_PATCH_RANKS: " + str(FIX_PATCH_RANKS))
    print("FIX_FAILED_BUGS: " + str(FIX_FAILED_BUGS))
    with open("fix_success_bugs.txt", "a") as f:
        f.write("RUN ERROR BUGS:"+str(RUN_FAILED_BUGS)+"\n")
        for i in range(len(FIX_SUCCESS_BUGS)):
            rank_info = FIX_PATCH_RANKS[i]
            rank = rank_info.split("/")[-1]
            f.write("{}: {}\n".format(FIX_SUCCESS_BUGS[i], rank))


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
        # print(path_str)
        lines = open(path_str, 'r').readlines()
        for line in lines:
            line = line.strip()
            if len(line) == 0:
                continue
            proj, idnum = line.split("_")
            all_fixed_bug_ids.append(proj+"-"+idnum)
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
        ret[proj].append(int(idnum))
    for key in ret.keys():
        ret[key].sort()

    # target_repo = ["Csv", "Gson", "JacksonCore", "JacksonDatabind", "JacksonXml", "Jsoup", "JxPath", "Mockito"]
    # target_repo = ["Chart", "Lang", "Math"]
    target_repo = ["Time", "Closure"] # server: 5
    # target_repo = ["Chart", "Lang"] # server: 12
    # target_repo = ["Math"] # server: 12 new...
    # target_repo = ["Closure"] # server: 25
    bugids = []
    for key in target_repo:
        idnums = ret[key]
        for idnum in idnums:
            # server 5
            if key == "Closure" and (idnum < 100  or idnum > 133):
                continue
            # if key == "Math" and idnum < 23:
            #     continue

            # server 25
            # if key == "Closure" and idnum > 100:
            #     continue
            bugids.append(key+"_"+str(idnum))

    # print(ret)
    return bugids

def check_out(llmName):
    bugids = get_bugids(llmName)
    for bugid in bugids:
        Repo = bugid.split("_")[0]
        repo = Repo.lower()
        idnum = bugid.split("_")[1]
        if os.path.exists("/data/PLM4APR/tmp/defects4j_buggy/{repo}/{repo}_{id}_buggy".format(repo=repo, id=idnum)):
            continue
        Checkout_CMD = "defects4j checkout -p {Repo} -v {id}b -w /data/PLM4APR/tmp/defects4j_buggy/{repo}/{repo}_{id}_buggy"
        cmd = Checkout_CMD.format(Repo=Repo, repo=repo, id=idnum)
        print(cmd)
        os.system(cmd)

def run_tools_on_all(llmName):
    bugids = get_bugids(llmName)
    RUN_APR_TOOL_CMD = "java -jar GenPat.jar repair -d4j {bugid} -d4jhome /data/PLM4APR/tmp/defects4j_buggy/ -modelname {modelName}"
    CORRECT_FIXES_ORACLE = "Correct fixed!!! Patch Rank:"

    RUN_SUCCESS_BUGS = []
    FIX_PATCH_RANKS = []
    RUN_FAILED_BUGS = []
    FIX_SUCCESS_BUGS = []
    FIX_FAILED_BUGS = []
    if llmName == "gpt":
        llmName = "gpt35"
    for bugid in bugids:
        print("Current fix is {id}| {index}/{all}".format(id=bugid, index = bugids.index(bugid), all=len(bugids)))
        bugid = bugid.lower()
        # bugid = bugid.split("-")[0] + "_" + bugid.split("-")[1]
        print(RUN_APR_TOOL_CMD.format(bugid=bugid, modelName=llmName))
        result = subprocess.run(RUN_APR_TOOL_CMD.format(bugid=bugid, modelName=llmName), shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        if result.returncode != -1:
            RUN_SUCCESS_BUGS.append(bugid)
            out_info = result.stdout
            if CORRECT_FIXES_ORACLE in out_info:
                out_info = out_info.split(CORRECT_FIXES_ORACLE)[1]
                FIX_SUCCESS_BUGS.append(bugid)
                rank_info = out_info.split("\n")[0].strip()
                FIX_PATCH_RANKS.append(rank_info)
                print(out_info)
            else:
                #print(out_info)
                print("Failed to fix bugid: " + bugid)
                FIX_FAILED_BUGS.append(bugid)
        else:
            RUN_FAILED_BUGS.append(bugid)
            print("Failed to run APR tool on bugid: " + bugid)
            print(result.stdout)

    print("RUN_SUCCESS_BUGS: " + str(RUN_SUCCESS_BUGS))
    print("RUN_FAILED_BUGS: " + str(RUN_FAILED_BUGS))
    print("FIX_SUCCESS_BUGS: " + str(FIX_SUCCESS_BUGS))
    print("FIX_PATCH_RANKS: " + str(FIX_PATCH_RANKS))
    print("FIX_FAILED_BUGS: " + str(FIX_FAILED_BUGS))
    with open("fix_success_bugs.txt", "a") as f:
        f.write("RUN ERROR BUGS:"+str(RUN_FAILED_BUGS)+"\n")
        for i in range(len(FIX_SUCCESS_BUGS)):
            rank_info = FIX_PATCH_RANKS[i]
            rank = rank_info.split("/")[-1]
            f.write("{}: {}\n".format(FIX_SUCCESS_BUGS[i], rank))

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("-m", "--model", help="model name", type=str, default="gpt35")
    parser.add_argument("-v", "--version", help="d4j version", type=str, default="10")
    parser.parse_args()
    args = parser.parse_args()
    llmName = args.model
    version = args.version
    # run_apr_tools(llmName, version)
    check_out(llmName)
    run_tools_on_all(llmName)