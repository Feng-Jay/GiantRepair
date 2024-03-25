import subprocess
import os
import logging

result_on_gpt35 ={
    "closure":[33, 55, 114, 118, 161],
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
    "closure":[5,15],
    "lang":[55],
    "math":[58,85]
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

def checkout_expressApr():
    list_result_dict = [result_on_gpt35_20]
    bugs = []
    for dict_result in list_result_dict:
        for key in dict_result.keys():
            for idnum in dict_result[key]:
                if key.lower() == "jacksoncore":
                    key = "JacksonCore"
                elif key.lower() == "jacksondatabind":
                    key = "JacksonDatabind"
                elif key.lower() == "jacksonxml":
                    key = "JacksonXml"
                elif key.lower() == "jxpath":
                    key = "JxPath"
                else:
                    key = key.capitalize()
                bugs.append("{}-{}".format(key, idnum))
    print(bugs)
    
    CheckOutCMD = "cd /data/PLM4APR/ExpressAPR " + "&& ./expapr-cli init -i defects4j -b {bugid} -w /data/PLM4APR/tmp/exapr/{proj}/{proj}_{idnum}_buggy/ -j 20 -d trivial"
    
    for bug in bugs:
        print(bug)
        if os.path.exists("/data/PLM4APR/tmp/exapr/{proj}/{proj}_{idnum}_buggy/repo-buggy-19".format(proj=bug.split("-")[0].lower(), idnum=bug.split("-")[1])):
            continue
        proj, idnum = bug.split("-")
        proj = proj.lower()
        cmd = CheckOutCMD.format(bugid=bug, proj=proj, idnum=idnum)    
        print("Checking out bugid: {}".format(bug))
        result = subprocess.run(cmd, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        stdout_output = result.stdout
        stderr_output = result.stderr
        return_code = result.returncode
        if return_code != 0:
            logging.error(stdout_output)
            logging.error(stderr_output)

if __name__ == "__main__":
   checkout_expressApr() 
