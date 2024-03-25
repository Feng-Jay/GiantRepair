# trying to validate the generated patches by multi-threading
import threading
import argparse
import subprocess
import os
import logging
import time
import json

thread_amount = 10 # default thread amount
working_directory = "/data/PLM4APR/tmp/defects4j_buggy_parallel/"

def checkout_patch(bugid):
    current_working_directory = working_directory + bugid + "_buggy_"
    if os.path.exists(current_working_directory + "1"):
        return
    threads = []
    for i in range(0, thread_amount):
        current_working_directory_tmp = current_working_directory + str(i) + "/"
        thread = threading.Thread(target=parallel_checkout, args=(i, bugid, current_working_directory_tmp))
        threads.append(thread)
        thread.start()
    for thread in threads:
        thread.join()
    

def parallel_checkout(tid, bugid, workdir):
    print("Thread {} is checking out bugid: {}".format(tid, bugid))
    proj, idnum = bugid.split("_")
    proj = proj.capitalize()
    CheckOut_CMD = "defects4j checkout -p {proj} -v {idnum}b -w {workdir}".format(proj=proj, idnum=idnum, workdir=workdir)
    try:
        subprocess.run(CheckOut_CMD, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
    except subprocess.CalledProcessError as e:
        print(f"Error occurred while running command: {CheckOut_CMD}\n{e.output}")
    print("Thread {} finished checkout".format(tid))
    return

def parallel_test(tid, workdir, jsonfiles):
    COMPILE_CMD = "defects4j compile -w {workdir}".format(workdir=workdir)
    TEST_CMD = "defects4j test -w {workdir}".format(workdir=workdir)
    jsontmp = json.load(open(jsonfiles[0], "r"))
    originalContent = jsontmp["context_above"] + "\n" + jsontmp["patches"][0] + "\n" + jsontmp["context_below"]
    currentFilePath = os.path.join(workdir, jsontmp["filename"])
    ids = []
    for jsonfile in jsonfiles:
        patchid = jsonfile.split("/")[-1].split(".")[0]
        jsonObject = json.load(open(jsonfile, "r"))
        patchContent = jsonObject["context_above"] + "\n" + jsonObject["patches"][0] + "\n" + jsonObject["context_below"]
        with open(currentFilePath, "w") as f:
            f.write(patchContent)
        compile_outcome = subprocess.Popen(COMPILE_CMD, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        stdout, stderr = compile_outcome.communicate()
        if compile_outcome.returncode != 0:
            print(patchid + " Compile Error!")
            continue
        test_outcome = subprocess.Popen(TEST_CMD, shell=True, stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True)
        stdout, stderr = test_outcome.communicate()
        if test_outcome.returncode != 0 or not ("Failing tests: 0" in stdout):
            print(patchid + " Test Error!")
            continue
        ids.append(patchid)
        print(patchid + " Test Passed!")
        print(stdout, stderr)
    with open(currentFilePath, "w") as f:
        f.write(originalContent)
    with open(workdir + "test_result.txt", "w") as f:
        f.write("\n".join(ids))
    print("Thread {} finished testing".format(tid))

def valid_patches(bugid):
    files = os.listdir("/data/PLM4APR/GenPat/patches/")
    counter = 0
    for file in files:
        if os.path.isfile("/data/PLM4APR/GenPat/patches/"+file) and file.endswith(".json"):
            counter += 1

    logging.info("There are {} valid patches for bugid: {}".format(counter, bugid))
    piece = (int)(counter / thread_amount)
    remainder = counter % thread_amount
    piece = piece if remainder == 0 else piece + 1
    threads = []
    beginTime = time.time()
    for i in range(0, thread_amount):
        start = i * piece
        end = (i + 1) * piece
        if start >= counter:
            break
        if end > counter:
            end = counter
        current_working_directory = working_directory + bugid + "_buggy_" + str(i) + "/"
        jsonfiles = []
        for j in range(start, end):
            jsonfiles.append("/data/PLM4APR/GenPat/patches/result{}.json".format(j))
        thread = threading.Thread(target=parallel_test, args=(i, current_working_directory, jsonfiles))
        threads.append(thread)
        thread.start()
    for thread in threads:
        thread.join()
    endTime = time.time()
    logging.info("Total time cost: {}s".format(endTime - beginTime))

if __name__ == "__main__":
    argparser = argparse.ArgumentParser()
    argparser.add_argument("-bugid", type=str, help="the bug id, format like: chart_1")
    
    args = argparser.parse_args()
    bugid = args.bugid
    checkout_patch(bugid)
    valid_patches(bugid)