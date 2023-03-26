import {beforeAll, describe, expect, test} from '@jest/globals';
import { JqlApi } from '@/api/jqlApi'

const jqlApi = new JqlApi('http://localhost:7007/api/jql/starwars/episode')
describe('Insert/Delete test', () => {

  async function clear_garbage(filter) {
    const garbage = (await jqlApi.find(filter)).content;
    let ids = '';
    if (garbage.length > 0) {
      for (const episode of garbage) {
        if (ids.length > 0) ids += ','
        ids += episode.title;
      }
      await jqlApi.delete(ids)
    }
  }

  describe('single entity insert/delete test',  () => {
    const episode_data = {
      title: "Test-E1",
      published: ("2023-03-23 10:30:00"),
    }
    let episode;

    beforeAll(async () => {
      await clear_garbage({ title: episode_data.title });
      episode = await jqlApi.insert(episode_data);
    });

    test('insert', async () => {
      expect(episode.title).toBe(episode_data.title);
      // expect(new Date(episode.published)).toBe(entities[0].published)
    });

    test('should throw error on conflict', async () => {
      await expect(async () => {
        const episode = await jqlApi.insert(episode);
      }).rejects.toThrowError();
    });

    test('delete test entity', async () => {
      await jqlApi.delete(episode.title);
    });

  });


  describe('batch insert/delete test', () => {

    const entity_data = [
      {
        title: "Test-E2-1",
        published: ("2023-03-23 10:30:00"),
      }, {
        title: "Test-E2-2",
        published: ("2023-03-23 10:30:00"),
      }
    ];

    let episode_map = {};

    beforeAll(async () => {
      await clear_garbage({ 'title@like': 'Test-E2-%' });

      const idList = await jqlApi.insertAll(entity_data);
      const res = await jqlApi.find({title: idList})
      const episodes = res.content;
      expect(episodes.length).toBe(entity_data.length);
      for (const episode of episodes) {
        episode_map[episode.title] = episode;
      }
    })

    test('Insert new', async () => {
      for (const episode of entity_data) {
        expect(episode.title).toBe(episode_map[episode.title].title);
        // expect(episode.published).toBe(episode_map[idx].published)
      }
    });

    test('should throw error on conflict', async () => {
      await expect(async () => {
        const episode = await jqlApi.insertAll(entity_data);
      }).rejects.toThrowError();
    });

    test('Ignore on conflict', async () => {
      const idList = await jqlApi.insertAll(entity_data, 'ignore');
      const res = await jqlApi.find({title: idList})
      const episodes = res.content;
      for (const episode of episodes) {
        expect(episode.title).toBe(episode_map[episode.title].title);
        expect(episode.id).toBe(episode_map[episode.title].id);
        // expect(episode.published).toBe(episode_map[idx].published)
      }
    });

    test('Insert or Update', async () => {
      for (const episode of entity_data) {
        episode.published = "2023-03-31 10:30:00"
      }
      const idList = await jqlApi.insertAll(entity_data, 'update');
      const res = await jqlApi.find({title: idList})
      const episodes = res.content;
      for (const episode of episodes) {
        expect(episode.title).toBe(episode_map[episode.title].title);
        expect(episode.id).toBe(episode_map[episode.title].id);
        // expect(episode.published).toBe(episode_map[idx].published)
      }
    });

    test('delete it', async () => {
      const titles = Object.keys(episode_map);
      let episodes = (await jqlApi.find({ title: titles } )).content;
      expect(episodes.length).toBe(titles.length);

      await jqlApi.delete(titles);

      episodes = (await jqlApi.find({ title: titles } )).content;
      expect(episodes.length).toBe(0)
    });
  });
});

