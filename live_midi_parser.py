import mido, keyboard

def note_num_to_str(note_num):
  """Convert MIDI note number to a readable string representation."""
  notes = ['C', 'C#', 'D', 'D#', 'E', 'F', 'F#', 'G', 'G#', 'A', 'A#', 'B']
  return notes[note_num % 12] + str(note_num // 12 - 1)

def main():
  # Startup Message
  print('Welcome to the Live MIDI Parser! Press ESC to stop.\n')

  # Determine available MIDI input ports
  ports = mido.get_input_names()
  ports_str = str(ports).replace("[", "").replace("]", "")
  print(f'Available MIDI ports are {ports_str}')

  if not ports:
    print('No MIDI ports were found. Please connect your MIDI device and try again.')
    return
  
  # Prompt user to select a MIDI port to listen on
  print('\nPlease select a MIDI port to listen on:')
  for i, port in enumerate(ports):
    print(f'{i+1}. {port}')
  
  port_index = int(input('\nEnter the port number: '))

  with mido.open_input(ports[0]) as inport:
    print(f'\nListening to MIDI port {ports[0]}\n')
    
    try:
      while(True):
        if keyboard.is_pressed('esc'):
          print('\nExiting...')
          return

        for msg in inport.iter_pending():
          if msg.type == 'note_on':
            note_name = note_num_to_str(msg.note)
            print(f'Note ON: {note_name}')
          elif msg.type == 'note_off':
            note_name = note_num_to_str(msg.note)
            print(f'Note OFF: {note_name}')

    except KeyboardInterrupt:
      print('\nExiting...')
      return
    
    finally:
      inport.close()
      print('Exited gracefully.')

if __name__ == '__main__':
  main()
