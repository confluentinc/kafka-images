#!/usr/bin/env python3
import json
import os
import sys

def format_graalvm_json(data):
    """Custom JSON formatter that matches GraalVM's exact formatting"""
    if isinstance(data, list):
        if not data:
            return "[]"
        
        lines = ["["]
        for i, item in enumerate(data):
            if isinstance(item, dict):
                lines.append("{")
                # Add object properties with 2-space indentation
                for j, (key, value) in enumerate(item.items()):
                    comma = "," if j < len(item) - 1 else ""
                    if isinstance(value, str):
                        lines.append(f'  "{key}":"{value}"{comma}')
                    elif isinstance(value, bool):
                        lines.append(f'  "{key}":{str(value).lower()}{comma}')
                    elif isinstance(value, list):
                        # Handle arrays within objects
                        if not value:
                            lines.append(f'  "{key}":[]{comma}')
                        else:
                            lines.append(f'  "{key}":[')
                            for k, arr_item in enumerate(value):
                                arr_comma = "," if k < len(value) - 1 else ""
                                if isinstance(arr_item, str):
                                    lines.append(f'    "{arr_item}"{arr_comma}')
                                else:
                                    lines.append(f'    {json.dumps(arr_item, separators=(",", ":"))}{arr_comma}')
                            lines.append(f'  ]{comma}')
                    else:
                        lines.append(f'  "{key}":{json.dumps(value, separators=(",", ":"))}{comma}')
                
                # Closing brace with comma if not last item
                closing_comma = "," if i < len(data) - 1 else ""
                lines.append(f"}}{closing_comma}")
            else:
                # Handle non-object array items
                closing_comma = "," if i < len(data) - 1 else ""
                lines.append(f"{json.dumps(item, separators=(',', ':'))}{closing_comma}")
        
        lines.append("]")
        return "\n".join(lines)
    
    elif isinstance(data, dict):
        return json.dumps(data, indent=2, separators=(',', ':'))
    
    else:
        return json.dumps(data, separators=(',', ':'))

def merge_graalvm_configs(file1, file2):
    """Merge two GraalVM config JSON files, removing duplicates"""
    
    with open(file1, 'r') as f1:
        config1 = json.load(f1)
    with open(file2, 'r') as f2:
        config2 = json.load(f2)
    
    # For array-based configs (most GraalVM configs), combine and dedupe
    if isinstance(config1, list) and isinstance(config2, list):
        combined = []
        seen = set()
        
        for item in config1 + config2:
            item_str = json.dumps(item, sort_keys=True)
            if item_str not in seen:
                seen.add(item_str)
                combined.append(item)
        
        print(f"Merged: {len(config1)} + {len(config2)} = {len(combined)} unique entries")
        return combined
    
    # For object-based configs, merge keys
    elif isinstance(config1, dict) and isinstance(config2, dict):
        merged = config1.copy()
        for key, value in config2.items():
            if key in merged:
                if isinstance(merged[key], list) and isinstance(value, list):
                    # Merge arrays within objects
                    combined_list = []
                    seen = set()
                    for item in merged[key] + value:
                        item_str = json.dumps(item, sort_keys=True)
                        if item_str not in seen:
                            seen.add(item_str)
                            combined_list.append(item)
                    merged[key] = combined_list
            else:
                merged[key] = value
        return merged
    
    # Default to newer config if types don't match
    return config2

def main():
    # Paths
    base_dir = "/Users/snehawadhwa/Documents/repos/kafka-images"
    set1_dir = f"{base_dir}/server-native/native-image-configs"
    set2_dir = f"{base_dir}/examples/confluent-server-oauth/graalvm-manifests-800"
    output_dir = f"{base_dir}/examples/confluent-server-oauth/merged-graalvm-configs"
    
    # Create output directory
    os.makedirs(output_dir, exist_ok=True)
    
    # Config files to merge
    config_files = [
        "reflect-config.json",
        "resource-config.json", 
        "proxy-config.json",
        "jni-config.json",
        "serialization-config.json",
        "predefined-classes-config.json"
    ]
    
    print("🔄 Merging GraalVM configurations...")
    print(f"Set 1 (Basic): {set1_dir}")
    print(f"Set 2 (OAuth): {set2_dir}")
    print(f"Output: {output_dir}")
    print("-" * 60)
    
    for config_file in config_files:
        file1 = f"{set1_dir}/{config_file}"
        file2 = f"{set2_dir}/{config_file}"
        output_file = f"{output_dir}/{config_file}"
        
        if os.path.exists(file1) and os.path.exists(file2):
            print(f"📄 Merging {config_file}...")
            merged_config = merge_graalvm_configs(file1, file2)
            
            with open(output_file, 'w') as f:
                f.write(format_graalvm_json(merged_config))
            
            print(f"✅ Saved: {output_file}")
            
        elif os.path.exists(file2):
            print(f"📄 Copying {config_file} (only in OAuth set)...")
            with open(file2, 'r') as f:
                config = json.load(f)
            with open(output_file, 'w') as f:
                f.write(format_graalvm_json(config))
                
        elif os.path.exists(file1):
            print(f"📄 Copying {config_file} (only in basic set)...")
            with open(file1, 'r') as f:
                config = json.load(f)
            with open(output_file, 'w') as f:
                f.write(format_graalvm_json(config))
        
        print()
    
    print("✨ Merge complete! Use the configs in:", output_dir)

if __name__ == "__main__":
    main() 