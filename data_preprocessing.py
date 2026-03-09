import json

file_path = 'refined_validation.json'
new_item = []

with open(file_path, 'r', encoding='utf-8') as f:
    data = json.load(f)

    for item in data:
        item["emotion"] = item["emotion"][:2]
    
new_file_path = 'final_validation.json'

with open(new_file_path, 'w', encoding='utf-8') as f:
    json.dump(data, f, indent=4, ensure_ascii=False)

print('작업완료!')
        


