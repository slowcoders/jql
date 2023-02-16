import {describe, expect, test} from '@jest/globals';
import { jqlApi } from '@/api/jqlApi'

describe('Join Test', () => {
  describe('Advanced Join', () => {
    test('Find friends of Han Solo', async () => {
      const filter = {
        "name" : "Han Solo",
        "friend_": {} 
      }      
      const res = await jqlApi.find(filter);
      const characters = res.content;
      expect(characters.length).toBe(1);
      expect(characters[0].friend_.length).toBeGreaterThanOrEqual(3);
    });

    test('Find friends of Han Solo with joined query', async () => {
      const filter = {
        "name" : "Han Solo",
        "friend_": { "starship_": { "length@ge": 10 } }
      }
      const res = await jqlApi.find(filter);
      const characters = res.content;
      expect(characters.length).toBe(1);
      expect(characters[0].friend_.length).toBe(1);
    });
  });
});

