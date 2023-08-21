import csv
import os
import sys

def parse_csv(file_path, field_names):
    """Parse a CSV file and extract specified fields."""
    parsed_data = []

    # Check if the file exists
    if not os.path.exists(file_path):
        print(f"Error: {file_path} not found.")
        return parsed_data

    try:
        with open(file_path, 'r', newline='', encoding='utf-8') as csv_file:
            csv_reader = csv.DictReader(csv_file)
            
            for row in csv_reader:
                parsed_row = {field: row.get(field, None) for field in field_names}
                parsed_data.append(parsed_row)
    except csv.Error as e:
        print(f"Error reading {file_path}: {e}")

    return parsed_data

def display_data(data):
    """Display parsed data."""
    for row in data:
        print(row)

if __name__ == "__main__":
    # Check if there's a command line argument for the file path
    if len(sys.argv) > 1:
        file_path = sys.argv[1]
    else:
        # Ask the user for the file path
        file_path = input("Please enter the path to the CSV file: ")

    fields_to_extract = [
        "icao24", "registration", "manufacturericao", "manufacturername", 
        "model", "typecode", "serialnumber", "linenumber", "icaoaircrafttype",
        "operator", "operatorcallsign", "operatoricao", "operatoriata", "owner",
        "testreg", "registered", "reguntil", "status", "built", "firstflightdate",
        "seatconfiguration", "engines", "modes", "adsb", "acars", "notes", "categoryDescription"
    ]

    parsed_data = parse_csv(file_path, fields_to_extract)
    
    display_data(parsed_data)
