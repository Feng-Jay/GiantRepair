import os

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

def compare(diffsFile, groundTruthFile):
    diffs = open(diffsFile, "r").readlines()
    diffs.pop(0)
    tmp = ""
    allDiffs = []
    for line in diffs:
        if line.startswith("Following diff"):
            allDiffs.append(tmp)
            tmp = ""
            continue
        tmp += line
    allDiffs.append(tmp)
    groundTruthLines = open(groundTruthFile, "r").readlines()
    groundTruth = ""
    for line in groundTruthLines:
        if len(line.strip()) == 0:
            continue
        groundTruth += line
    if groundTruth in allDiffs:
        return allDiffs.index(groundTruth)
    else:
        return -1
     
def getRes():
    models = ['gpt35', 'starcoder', 'codellama', 'llama']
    result_10 = [result_on_gpt35, result_on_starcoder, result_on_codellama, result_on_llama]
    result_20 = [result_on_gpt35_20, result_on_starcoder_20, result_on_codellama_20, result_on_llama_20]
    for i in range(len(models)):
        model = models[i]
        result = result_20[i]
        dstFile = "./repair-result/ranks/{model}_20.txt".format(model = model)
        if not os.path.exists(os.path.dirname(dstFile)):
            os.makedirs(os.path.dirname(dstFile))
        with open(dstFile, 'w') as f: 
            for proj in result:
                for bugid in result[proj]:
                    repair_result_path = "./repair-result/patch_"+model+"_20/"+proj+"/"+str(bugid)+".diff"
                    ground_truth_path = "./resources/groundTruth/"+model+"/"+proj+"_"+str(bugid)+".txt"
                    if not os.path.exists(repair_result_path):
                        # f.write("{proj}_{bugid}: NOTFOUND\n".format(proj = proj, bugid = bugid))
                        continue
                    rank = compare(repair_result_path, ground_truth_path)
                    # f.write("{proj}_{bugid}: {rank}\n".format(proj = proj, bugid = bugid, rank = rank))
                    if proj.lower() == "jacksoncore":
                        proj = "JacksonCore"
                    elif proj.lower() == "jacksondatabind":
                        proj = "JacksonDatabind"
                    elif proj.lower() == "jacksonxml":
                        proj = "JacksonXml"
                    elif proj.lower() == "jxpath":
                        proj = "JxPath"
                    else:
                        proj = proj.capitalize()
                    f.write("{proj}_{bugid}: {rank}\n".format(proj = proj, bugid = bugid, rank = rank))
                    # f.write("{proj}_{bugid}\n".format(proj = proj, bugid = bugid))
        # break

def clear_logs(model_name):
    # this script is used to clean duplicate logs
    if model_name == "gpt35":
        result_10 = result_on_gpt35
        result_20 = result_on_gpt35_20
    elif model_name == "starcoder":
        result_10 = result_on_starcoder
        result_20 = result_on_starcoder_20
    elif model_name == "codellama":
        result_10 = result_on_codellama
        result_20 = result_on_codellama_20
    elif model_name == "llama":
        result_10 = result_on_llama
        result_20 = result_on_llama_20
    logs_10_path = "./repair-result/log_{mname}_10/".format(mname = model_name)
    remove_files(logs_10_path, result_10)
    logs_20_path = "./repair-result/log_{mname}_20/".format(mname = model_name)
    remove_files(logs_20_path, result_20)

def remove_files(logs_path, result):
    reserved_files = []
    for key in result:
        for idnum in result[key]:
            reserved_files.append(os.path.join(logs_path, key.lower(), str(idnum)+".txt"))
    # print(reserved_files)

    # delete unnecessary files
    for root, dirs, files in os.walk(logs_path):
        for file in files:
            file_path = os.path.join(root, file)
            if file_path not in reserved_files:
                os.remove(file_path)
    # then remove empty dirs
    for root, dirs, files in os.walk(logs_path, topdown=False):
        if not dirs and not files:
            print(f"Removing empty directory: {root}")
            try:
                os.rmdir(root)
            except OSError as e:
                print(f"Error: {root} : {e.strerror}")
    

if __name__ == "__main__":
    # getRes()
    clear_logs("llama")