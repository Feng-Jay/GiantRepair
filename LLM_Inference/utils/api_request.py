import openai
import time

def create_gpt4_config(message, 
                       model = "gpt-4-1106-preview",
                       stop = "# Provide a fix for the buggy function",
                       max_tokens=3000,
                       top_p = 1,
                       temperature = 0):
    return {
        "model": model,
        "messages": [{"role": "user", "content" :message}],
        "max_tokens": max_tokens,
        "top_p": top_p,
        "temperature": temperature,
        "stop": stop
    }

def create_openai_config(message,
                         model="gpt-3.5-turbo-0301",
                         stop="# Provide a fix for the buggy function",
                         max_tokens=3000,
                         top_p=1,
                         temperature=0):
    return {
        "model": model,
        "messages": [{"role": "user", "content" :message}],
        "max_tokens": max_tokens,
        "top_p": top_p,
        "temperature": temperature,
        "stop": stop
    }


def create_openai_config_suffix(prompt, suffix,
                                engine_name="code-davinci-002",
                                max_tokens=500,
                                top_p=1,
                                temperature=0):
    return {
        "engine": engine_name,
        "prompt": prompt,
        "max_tokens": max_tokens,
        "top_p": top_p,
        "temperature": temperature,
        "suffix": suffix
    }


def create_openai_config_single(prompt, stop,
                                engine_name="code-davinci-002",
                                max_tokens=100,
                                top_p=1,
                                temperature=0):
    return {
        "engine": engine_name,
        "prompt": prompt,
        "max_tokens": max_tokens,
        "top_p": top_p,
        "temperature": temperature,
        "logprobs": 1,
        "stop": stop
    }


# Handles requests to OpenAI API
def request_engine(config):
    ret = None
    while ret is None:
        try:
            ret = openai.ChatCompletion.create(**config)
        except openai.error.InvalidRequestError as e:
            print(e)
            if "Please reduce your prompt" in str(e):
                config['max_tokens'] = config['max_tokens'] - 200
                if config['max_tokens'] < 100:
                    return None
            else:
                return None
        except openai.error.RateLimitError as e:
            print("Rate limit exceeded. Waiting...")
            time.sleep(60) # wait for a minute
        except openai.error.APIConnectionError as e:
            print("API connection error. Waiting...")
            time.sleep(5)  # wait for a minute
        except:
            print("Unknown error. Waiting...")
            time.sleep(5)  # wait for a minute
    return ret
