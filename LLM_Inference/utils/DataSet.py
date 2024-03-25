import os
import json

class DataSet:
    
    def __init__(self, bugList, bugFiles) -> None:
        self.bugList  = json.load(open(bugList, 'r'))
        self.bugFiles = json.load(open(bugFiles, 'r'))
        return
    
    def getBugList(self):
        ret = {}
        for k in self.bugList.keys():
            proj = k.split('-')[0]
            numid= int(k.split('-')[1])
            if proj not in ret.keys():
                ret[proj] = [numid]
            else:
                ret[proj].append(numid)
        ret = {k: sorted(v) for k, v in ret.items()}
        return ret
    
    def getBugFiles(self):
        return self.bugFiles
    
    def getOneBugLine(self, bug):
        return self.bugList[bug]["start"], self.bugList[bug]["end"]
    
    def getOneBugFile(self, bug):
        return self.bugFiles[bug]
    
    def getBuggyCode(self, bug):
        return self.bugList[bug]["buggy"]
    
    def getFixCode(self, bug):
        return self.bugList[bug]["fix"]