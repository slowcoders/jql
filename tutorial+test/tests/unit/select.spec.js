import {describe, expect, test} from '@jest/globals';
import { jqlApi } from '@/api/jqlApi'

describe('Select Test', () => {
  test('Find first', async () => {
    const filter = {
      "name@like": "Luke%"
    }
    const character = await jqlApi.top(filter);

    expect(character.name).toBe("Luke Skywalker");

    expect(character.name).toMatch("Sky");
    expect(character.name).toMatch(/Luke Skywalker/);
    expect(character.name).toMatch(/Luke .*/);

    expect(character.name.indexOf("Luke") == 0).toBeTruthy();
    expect(character.name.startsWith("Luke")).toBeTruthy();
  });

  test('Select PrimaryKeys only', async () => {
    const character = await jqlApi.top(null, { select: "0" });
    for (const k in character) {
      expect(k).toBe('id');
    }
  });

  describe('Select PrimaryKeys and Name', () => {
    test('Implicit ID selection for array node', async () => {
      const character = await jqlApi.top(null, {select: "name"});
      for (const k in character) {
        expect(k == 'id' || k == 'name').toBeTruthy();
      }
    })

    test('Explicit ID selection with other leaf property', async () => {
      const character = await jqlApi.top(null, {select: "0, name"});
      expect(character.id).not.toBeUndefined();
      expect(character.name).not.toBeUndefined();
      for (const k in character) {
        expect(k == 'id' || k == 'name').toBeTruthy();
      }
    });
  });

  test('Select Episodes of Luke Skywalker)', async () => {
    const filter = {
      "name@like": "Luke%"
    }
    const character = await jqlApi.top(filter, { select: "episode_(title)" });
    expect(character.id).not.toBeUndefined();
    expect(character.episode_).not.toBeUndefined();
    for (const k in character) {
      expect(k == 'id' || k == 'episode_').toBeTruthy();
    }
    for (const episode of character.episode_) {
      for (const k in episode) {
        expect(k == 'title').toBeTruthy();
      }
    }
  });

  test('Select Episodes and Starships of Luke Skywalker)', async () => {
    const filter = {
      "name@like": "Luke%"
    }
    const character = await jqlApi.top(filter, { select: "episode_, starship_" });
    expect(character.id).not.toBeUndefined();
    expect(character.episode_).not.toBeUndefined();
    expect(character.starship_).not.toBeUndefined();
    for (const k in character) {
      expect(k == 'id' || k == 'episode_' || k == 'starship_').toBeTruthy();
    }
  });


});

